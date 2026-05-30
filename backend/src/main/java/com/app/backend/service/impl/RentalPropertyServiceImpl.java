package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.rentals.RentalPropertyRequest;
import com.app.backend.dto.rentals.RentalPropertyResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.LinkedEntityType;
import com.app.backend.entity.Owner;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.RentalPropertySpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.PropertyImageService;
import com.app.backend.service.RentalPropertyService;
import com.app.backend.service.rentals.RentalPropertyMapper;
import com.app.backend.support.SoftDelete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RentalPropertyServiceImpl implements RentalPropertyService {

    private static final Logger log = LoggerFactory.getLogger(RentalPropertyServiceImpl.class);

    private final RentalPropertyRepository rentalPropertyRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RentalPropertyMapper rentalPropertyMapper;
    private final ActivityLogService activityLogService;
    private final PropertyImageService propertyImageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalPropertyResponse> list(
            UserPrincipal viewer,
            Optional<RentalPropertyType> type,
            Optional<RentalPropertyStatus> status,
            Optional<BigDecimal> minRent,
            Optional<BigDecimal> maxRent,
            Pageable pageable
    ) {
        validateInclusiveRange(minRent.orElse(null), maxRent.orElse(null), "rent range");
        Specification<RentalProperty> spec = Specification.where(SoftDeleteSpecifications.<RentalProperty>notDeleted())
                .and(RentalPropertySpecifications.visibleTo(viewer))
                .and(RentalPropertySpecifications.typeEquals(type.orElse(null)))
                .and(RentalPropertySpecifications.statusEquals(status.orElse(null)))
                .and(RentalPropertySpecifications.rentAmountBetween(minRent.orElse(null), maxRent.orElse(null)));
        return PageResponse.from(
                rentalPropertyRepository.findAll(spec, pageable)
                        .map(e -> rentalPropertyMapper.toResponse(e, viewer))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RentalPropertyResponse get(UserPrincipal viewer, Long id) {
        RentalProperty entity = rentalPropertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental property not found"));
        assertAgentCanSee(viewer, entity);
        return rentalPropertyMapper.toResponse(entity, viewer);
    }

    @Override
    @Transactional
    public RentalPropertyResponse create(UserPrincipal actor, RentalPropertyRequest request) {
        User actorEntity = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RentalProperty entity = new RentalProperty();
        applyCoreFields(entity, request);

        if (actor.getRole() == UserRole.AGENT) {
            if (request.getOwnerId() != null) {
                throw new AccessDeniedException("Agents cannot assign registry owners");
            }
            validateAgentSelfAssignment(actor, request.getAssignedAgentId());
            entity.setOwner(null);
            entity.setAssignedAgent(userRepository.getReferenceById(actor.getId()));
            entity.setCreatedBy(actorEntity);
        } else {
            if (request.getOwnerId() == null) {
                throw new IllegalArgumentException("ownerId is required");
            }
            entity.setOwner(resolveRegistryOwner(request.getOwnerId()));
            entity.setAssignedAgent(resolveAssignedAgent(request.getAssignedAgentId()));
            entity.setCreatedBy(actorEntity);
        }

        entity = rentalPropertyRepository.save(entity);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.RENTAL_PROPERTY, entity.getId());
        return rentalPropertyMapper.toResponse(entity, actor);
    }

    @Override
    @Transactional
    public RentalPropertyResponse update(UserPrincipal actor, Long id, RentalPropertyRequest request) {
        RentalProperty entity = rentalPropertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental property not found"));
        assertAgentCanSee(actor, entity);
        assertAgentCanMutate(actor, entity);

        applyCoreFields(entity, request);

        if (actor.getRole() == UserRole.AGENT) {
            if (request.getOwnerId() != null) {
                throw new AccessDeniedException("Agents cannot assign registry owners");
            }
            validateAgentSelfAssignment(actor, request.getAssignedAgentId());
        } else {
            if (request.getOwnerId() != null) {
                entity.setOwner(resolveRegistryOwner(request.getOwnerId()));
            }
            entity.setAssignedAgent(resolveAssignedAgent(request.getAssignedAgentId()));
        }

        entity = rentalPropertyRepository.save(entity);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.RENTAL_PROPERTY, entity.getId());
        return rentalPropertyMapper.toResponse(entity, actor);
    }

    @Override
    @Transactional
    public void delete(UserPrincipal actor, Long id) {
        if (actor.getRole() == UserRole.AGENT) {
            throw new AccessDeniedException("Agents cannot delete rental properties");
        }
        RentalProperty entity = rentalPropertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rental property not found"));
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.RENTAL_PROPERTY, id);
        propertyImageService.deleteAllForEntity(LinkedEntityType.RENTAL_PROPERTY, id);
        SoftDelete.mark(entity);
        rentalPropertyRepository.save(entity);
        log.info("Soft-delete rental property id={} by userId={}", id, actor.getId());
    }

    private static void assertAgentCanSee(UserPrincipal viewer, RentalProperty entity) {
        if (viewer.getRole() != UserRole.AGENT) {
            return;
        }
        boolean assigned = entity.getAssignedAgent() != null && entity.getAssignedAgent().getId().equals(viewer.getId());
        boolean created = entity.getCreatedBy() != null && entity.getCreatedBy().getId().equals(viewer.getId());
        if (!assigned && !created) {
            throw new ResourceNotFoundException("Rental property not found");
        }
    }

    private static void assertAgentCanMutate(UserPrincipal actor, RentalProperty entity) {
        if (actor.getRole() != UserRole.AGENT) {
            return;
        }
        if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Agents may only update rental properties they created");
        }
    }

    private static void validateAgentSelfAssignment(UserPrincipal actor, Long assignedAgentId) {
        if (assignedAgentId == null) {
            return;
        }
        if (!assignedAgentId.equals(actor.getId())) {
            throw new AccessDeniedException("Agents can only assign themselves as agent");
        }
    }

    private static void validateInclusiveRange(BigDecimal min, BigDecimal max, String label) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Invalid " + label + ": minimum exceeds maximum");
        }
    }

    private static void applyCoreFields(RentalProperty entity, RentalPropertyRequest request) {
        entity.setTitle(request.getTitle().trim());
        entity.setType(request.getType());
        entity.setAddress(request.getAddress().trim());
        entity.setRentAmount(request.getRentAmount());
        entity.setStatus(request.getStatus());
    }

    private Owner resolveRegistryOwner(Long ownerId) {
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
    }

    private User resolveAssignedAgent(Long assignedAgentId) {
        if (assignedAgentId == null) {
            return null;
        }
        User agent = userRepository.findById(assignedAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned agent not found"));
        if (agent.getRole() != UserRole.AGENT) {
            throw new IllegalArgumentException("assignedAgentId must reference a user with role AGENT");
        }
        return agent;
    }
}
