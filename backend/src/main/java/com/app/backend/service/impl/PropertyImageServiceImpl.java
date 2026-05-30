package com.app.backend.service.impl;

import com.app.backend.config.StorageProperties;
import com.app.backend.dto.media.PropertyImageResponse;
import com.app.backend.entity.LinkedEntityType;
import com.app.backend.entity.Plot;
import com.app.backend.entity.PropertyImage;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.UserRole;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.PropertyImageRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.PropertyImageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyImageServiceImpl implements PropertyImageService {

    private final PropertyImageRepository propertyImageRepository;
    private final PlotRepository plotRepository;
    private final RentalPropertyRepository rentalPropertyRepository;
    private final StorageProperties storageProperties;

    private Path rootPath;
    private Set<String> allowedMimeTypes;

    @PostConstruct
    void init() throws IOException {
        rootPath = Paths.get(storageProperties.getRoot()).toAbsolutePath().normalize();
        Files.createDirectories(rootPath);
        allowedMimeTypes = Arrays.stream(storageProperties.getAllowedContentTypes().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public PropertyImageResponse upload(UserPrincipal actor, LinkedEntityType entityType, Long entityId, MultipartFile file) {
        validateImage(file);
        assertCanModifyLinkedEntity(actor, entityType, entityId);

        String safeBase = sanitizeBaseName(file.getOriginalFilename());
        String ext = extensionOf(file.getOriginalFilename());
        String relative = entityType.name().toLowerCase(Locale.ROOT) + "/" + entityId + "/" + UUID.randomUUID() + ext;
        Path target = rootPath.resolve(relative).normalize();
        if (!target.startsWith(rootPath)) {
            throw new IllegalStateException("Invalid storage path");
        }
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        PropertyImage img = new PropertyImage();
        img.setLinkedEntityType(entityType);
        img.setLinkedEntityId(entityId);
        img.setFileName(safeBase + ext);
        img.setContentType(Objects.requireNonNullElse(file.getContentType(), "application/octet-stream").toLowerCase(Locale.ROOT));
        img.setStoragePath(relative.replace('\\', '/'));
        img.setByteSize(file.getSize());
        img.setCreatedAt(Instant.now());
        img = propertyImageRepository.save(img);
        return toResponse(img);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyImageResponse> list(UserPrincipal viewer, LinkedEntityType entityType, Long entityId) {
        assertCanViewLinkedEntity(viewer, entityType, entityId);
        return propertyImageRepository
                .findByLinkedEntityTypeAndLinkedEntityIdOrderByCreatedAtAsc(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ImageFilePayload loadFile(UserPrincipal viewer, Long imageId) {
        PropertyImage img = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        assertCanViewLinkedEntity(viewer, img.getLinkedEntityType(), img.getLinkedEntityId());
        Path path = rootPath.resolve(img.getStoragePath()).normalize();
        if (!path.startsWith(rootPath) || !Files.isRegularFile(path)) {
            throw new ResourceNotFoundException("Image file missing");
        }
        Resource resource = new FileSystemResource(path.toFile());
        return new ImageFilePayload(resource, img.getContentType());
    }

    @Override
    @Transactional
    public void delete(UserPrincipal actor, Long imageId) {
        PropertyImage img = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        assertCanModifyLinkedEntity(actor, img.getLinkedEntityType(), img.getLinkedEntityId());
        deleteFileQuietly(img.getStoragePath());
        propertyImageRepository.delete(img);
    }

    @Override
    @Transactional
    public void deleteAllForEntity(LinkedEntityType entityType, Long entityId) {
        List<PropertyImage> rows =
                propertyImageRepository.findByLinkedEntityTypeAndLinkedEntityIdOrderByCreatedAtAsc(entityType, entityId);
        for (PropertyImage img : rows) {
            deleteFileQuietly(img.getStoragePath());
            propertyImageRepository.delete(img);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > storageProperties.getMaxImageBytes()) {
            throw new IllegalArgumentException("File exceeds maximum allowed size");
        }
        String ct = file.getContentType();
        if (ct == null || ct.isBlank()) {
            throw new IllegalArgumentException("Content type is required");
        }
        ct = ct.toLowerCase(Locale.ROOT);
        if (!allowedMimeTypes.contains(ct) || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Only image uploads are allowed");
        }
    }

    private void assertCanViewLinkedEntity(UserPrincipal viewer, LinkedEntityType type, Long entityId) {
        if (viewer.getRole() == UserRole.DIRECTOR || viewer.getRole() == UserRole.ADMIN) {
            ensureEntityExists(type, entityId);
            return;
        }
        if (viewer.getRole() == UserRole.AGENT) {
            if (type == LinkedEntityType.PLOT) {
                Plot plot = plotRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
                if (!agentCanViewPlot(viewer, plot)) {
                    throw new ResourceNotFoundException("Plot not found");
                }
                return;
            }
            RentalProperty rental = rentalPropertyRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
            if (!agentCanViewRental(viewer, rental)) {
                throw new ResourceNotFoundException("Rental not found");
            }
            return;
        }
        throw new ResourceNotFoundException("Not found");
    }

    private void assertCanModifyLinkedEntity(UserPrincipal actor, LinkedEntityType type, Long entityId) {
        if (actor.getRole() == UserRole.DIRECTOR || actor.getRole() == UserRole.ADMIN) {
            ensureEntityExists(type, entityId);
            return;
        }
        if (actor.getRole() == UserRole.AGENT) {
            if (type == LinkedEntityType.PLOT) {
                Plot plot = plotRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
                if (!agentCanMutatePlot(actor, plot)) {
                    throw new org.springframework.security.access.AccessDeniedException("Agents may only modify images on plots they created");
                }
                return;
            }
            RentalProperty rental = rentalPropertyRepository.findById(entityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
            if (!agentCanMutateRental(actor, rental)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Agents may only modify images on rental properties they created");
            }
            return;
        }
        throw new ResourceNotFoundException("Not found");
    }

    private static boolean agentCanViewPlot(UserPrincipal viewer, Plot plot) {
        boolean assigned = plot.getAssignedAgent() != null && plot.getAssignedAgent().getId().equals(viewer.getId());
        boolean created = plot.getCreatedBy() != null && plot.getCreatedBy().getId().equals(viewer.getId());
        return assigned || created;
    }

    private static boolean agentCanViewRental(UserPrincipal viewer, RentalProperty rental) {
        boolean assigned = rental.getAssignedAgent() != null && rental.getAssignedAgent().getId().equals(viewer.getId());
        boolean created = rental.getCreatedBy() != null && rental.getCreatedBy().getId().equals(viewer.getId());
        return assigned || created;
    }

    private static boolean agentCanMutatePlot(UserPrincipal actor, Plot plot) {
        return plot.getCreatedBy() != null && plot.getCreatedBy().getId().equals(actor.getId());
    }

    private static boolean agentCanMutateRental(UserPrincipal actor, RentalProperty rental) {
        return rental.getCreatedBy() != null && rental.getCreatedBy().getId().equals(actor.getId());
    }

    private void ensureEntityExists(LinkedEntityType type, Long entityId) {
        if (type == LinkedEntityType.PLOT) {
            plotRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
        } else {
            rentalPropertyRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
        }
    }

    private void deleteFileQuietly(String storagePath) {
        try {
            Path p = rootPath.resolve(storagePath).normalize();
            if (p.startsWith(rootPath)) {
                Files.deleteIfExists(p);
            }
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    private static String sanitizeBaseName(String original) {
        String base = original == null ? "image" : original;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (base.isBlank()) {
            base = "image";
        }
        return base.length() > 120 ? base.substring(0, 120) : base;
    }

    private static String extensionOf(String original) {
        if (original == null || !original.contains(".")) {
            return ".bin";
        }
        String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (ext.length() > 12) {
            return ".bin";
        }
        return ext;
    }

    private PropertyImageResponse toResponse(PropertyImage img) {
        return PropertyImageResponse.builder()
                .id(img.getId())
                .linkedEntityType(img.getLinkedEntityType())
                .linkedEntityId(img.getLinkedEntityId())
                .fileName(img.getFileName())
                .contentType(img.getContentType())
                .byteSize(img.getByteSize())
                .createdAt(img.getCreatedAt())
                .downloadUrl("/api/v1/property-images/" + img.getId() + "/file")
                .build();
    }
}
