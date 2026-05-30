package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PlotRequest;
import com.app.backend.dto.plots.PlotResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.LinkedEntityType;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.PlotStatus;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.PlotSpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.PlotService;
import com.app.backend.service.PropertyImageService;
import com.app.backend.service.plots.PlotMapper;
import com.app.backend.support.SoftDelete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlotServiceImpl implements PlotService {

    private static final Logger log = LoggerFactory.getLogger(PlotServiceImpl.class);

    private final PlotRepository plotRepository;
    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final PlotMapper plotMapper;
    private final ActivityLogService activityLogService;
    private final PropertyImageService propertyImageService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlotResponse> list(
            UserPrincipal viewer,
            Optional<Long> phaseId,
            Optional<Long> khayabanId,
            Optional<PlotStatus> status,
            Optional<BigDecimal> minPrice,
            Optional<BigDecimal> maxPrice,
            Optional<BigDecimal> minSize,
            Optional<BigDecimal> maxSize,
            Pageable pageable
    ) {
        Long pId = phaseId.orElse(null);
        Long kId = khayabanId.orElse(null);
        validatePhaseKhayabanFilter(pId, kId);
        validateInclusiveRange(minPrice.orElse(null), maxPrice.orElse(null), "price range");
        validateInclusiveRange(minSize.orElse(null), maxSize.orElse(null), "size range");

        Specification<Plot> spec = Specification.where(SoftDeleteSpecifications.<Plot>notDeleted())
                .and(PlotSpecifications.visibleTo(viewer))
                .and(PlotSpecifications.phaseIdEquals(pId))
                .and(PlotSpecifications.khayabanIdEquals(kId))
                .and(PlotSpecifications.statusEquals(status.orElse(null)))
                .and(PlotSpecifications.priceBetween(minPrice.orElse(null), maxPrice.orElse(null)))
                .and(PlotSpecifications.sizeBetween(minSize.orElse(null), maxSize.orElse(null)));

        return PageResponse.from(
                plotRepository.findAll(spec, pageable).map(p -> plotMapper.toPlotResponse(p, viewer)));
    }

    @Override
    @Transactional(readOnly = true)
    public PlotResponse get(UserPrincipal viewer, Long id) {
        Plot plot = plotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
        assertAgentCanSee(viewer, plot);
        return plotMapper.toPlotResponse(plot, viewer);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public PlotResponse create(UserPrincipal actor, PlotRequest request) {
        Phase phase = phaseRepository.findById(request.getPhaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        Khayaban khayaban = khayabanRepository.findById(request.getKhayabanId())
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        ensureKhayabanBelongsToPhase(phase, khayaban);

        User actorEntity = userRepository.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Plot plot = new Plot();
        applyCoreFields(plot, request, phase, khayaban);

        if (actor.getRole() == UserRole.AGENT) {
            if (request.getOwnerId() != null) {
                throw new AccessDeniedException("Agents cannot assign registry owners");
            }
            plot.setOwner(null);
            validateAgentSelfAssignment(actor, request.getAssignedAgentId());
            plot.setAssignedAgent(userRepository.getReferenceById(actor.getId()));
            plot.setCreatedBy(actorEntity);
        } else {
            plot.setOwner(resolveRegistryOwner(request.getOwnerId()));
            plot.setAssignedAgent(resolveAssignedAgent(request.getAssignedAgentId()));
            plot.setCreatedBy(actorEntity);
        }

        plot = plotRepository.save(plot);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.PLOT, plot.getId());
        return plotMapper.toPlotResponse(plot, actor);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public PlotResponse update(UserPrincipal actor, Long id, PlotRequest request) {
        Plot plot = plotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
        assertAgentCanSee(actor, plot);
        assertAgentCanMutate(actor, plot);

        Phase phase = phaseRepository.findById(request.getPhaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        Khayaban khayaban = khayabanRepository.findById(request.getKhayabanId())
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        ensureKhayabanBelongsToPhase(phase, khayaban);

        applyCoreFields(plot, request, phase, khayaban);

        if (actor.getRole() == UserRole.AGENT) {
            if (request.getOwnerId() != null) {
                throw new AccessDeniedException("Agents cannot assign registry owners");
            }
            validateAgentSelfAssignment(actor, request.getAssignedAgentId());
            // Preserve staff-managed links for agents
        } else {
            plot.setOwner(resolveRegistryOwner(request.getOwnerId()));
            plot.setAssignedAgent(resolveAssignedAgent(request.getAssignedAgentId()));
        }

        plot = plotRepository.save(plot);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.PLOT, plot.getId());
        return plotMapper.toPlotResponse(plot, actor);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public void delete(UserPrincipal actor, Long id) {
        Plot plot = plotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plot not found"));
        if (actor.getRole() == UserRole.AGENT) {
            if (!actor.hasPermission(PermissionCodes.PLOT_DELETE)) {
                throw new AccessDeniedException("Missing permission PLOT_DELETE");
            }
            assertAgentCanMutate(actor, plot);
        } else if (actor.getRole() != UserRole.DIRECTOR && actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Cannot delete plot");
        }
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.PLOT, id);
        propertyImageService.deleteAllForEntity(LinkedEntityType.PLOT, id);
        SoftDelete.mark(plot);
        plotRepository.save(plot);
        log.info("Soft-delete plot id={} by userId={}", id, actor.getId());
    }

    private void validatePhaseKhayabanFilter(Long phaseId, Long khayabanId) {
        if (phaseId == null || khayabanId == null) {
            return;
        }
        Khayaban khayaban = khayabanRepository.findById(khayabanId)
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        if (!khayaban.getPhase().getId().equals(phaseId)) {
            throw new IllegalArgumentException("phaseId does not match the khayaban's phase");
        }
    }

    private static void validateInclusiveRange(BigDecimal min, BigDecimal max, String label) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Invalid " + label + ": minimum exceeds maximum");
        }
    }

    private static void ensureKhayabanBelongsToPhase(Phase phase, Khayaban khayaban) {
        if (!khayaban.getPhase().getId().equals(phase.getId())) {
            throw new IllegalArgumentException("Khayaban does not belong to the given phase");
        }
    }

    private static void assertAgentCanSee(UserPrincipal viewer, Plot plot) {
        if (viewer.getRole() != UserRole.AGENT) {
            return;
        }
        boolean assigned = plot.getAssignedAgent() != null && plot.getAssignedAgent().getId().equals(viewer.getId());
        boolean created = plot.getCreatedBy() != null && plot.getCreatedBy().getId().equals(viewer.getId());
        if (!assigned && !created) {
            throw new ResourceNotFoundException("Plot not found");
        }
    }

    private static void assertAgentCanMutate(UserPrincipal actor, Plot plot) {
        if (actor.getRole() != UserRole.AGENT) {
            return;
        }
        if (plot.getCreatedBy() == null || !plot.getCreatedBy().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Agents may only update plots they created");
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

    private void applyCoreFields(Plot plot, PlotRequest request, Phase phase, Khayaban khayaban) {
        plot.setPlotNumber(request.getPlotNumber().trim());
        plot.setSize(request.getSize());
        plot.setPrice(request.getPrice());
        plot.setStatus(request.getStatus());
        plot.setPhase(phase);
        plot.setKhayaban(khayaban);
    }

    private Owner resolveRegistryOwner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
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
