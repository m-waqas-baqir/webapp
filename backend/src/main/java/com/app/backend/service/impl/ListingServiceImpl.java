package com.app.backend.service.impl;

import com.app.backend.dto.listing.ListingRequest;
import com.app.backend.dto.listing.ListingResponse;
import com.app.backend.dto.listing.OwnerFullDetailResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Listing;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.ListingSpecifications;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.ListingService;
import com.app.backend.service.listing.ListingViewMapper;
import com.app.backend.support.SoftDelete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingServiceImpl.class);

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingViewMapper listingViewMapper;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> listFor(UserPrincipal viewer) {
        return listingRepository
                .findAll(
                        Specification.where(SoftDeleteSpecifications.<Listing>notDeleted())
                                .and(ListingSpecifications.visibleTo(viewer)))
                .stream()
                .map(l -> listingViewMapper.toResponse(l, viewer))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getFor(UserPrincipal viewer, Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        assertAgentCanSee(viewer, listing);
        return listingViewMapper.toResponse(listing, viewer);
    }

    @Override
    @Transactional
    public ListingResponse create(UserPrincipal actor, ListingRequest request) {
        validateAssignedAgent(request.getAssignedAgentId());
        Listing listing = new Listing();
        applyRequest(listing, request);
        listing = listingRepository.save(listing);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.LISTING, listing.getId());
        return listingViewMapper.toResponse(listing, actor);
    }

    @Override
    @Transactional
    public ListingResponse update(UserPrincipal actor, Long id, ListingRequest request) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        validateAssignedAgent(request.getAssignedAgentId());
        applyRequest(listing, request);
        listing = listingRepository.save(listing);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.LISTING, listing.getId());
        return listingViewMapper.toResponse(listing, actor);
    }

    @Override
    @Transactional
    public void delete(UserPrincipal actor, Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.LISTING, id);
        SoftDelete.mark(listing);
        listingRepository.save(listing);
        log.info("Soft-delete listing id={} by userId={}", id, actor.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerFullDetailResponse> listOwnerDetailsForDirector() {
        return listingRepository
                .findAll(Specification.where(SoftDeleteSpecifications.<Listing>notDeleted()))
                .stream()
                .map(listingViewMapper::toDirectorRow)
                .toList();
    }

    private static void assertAgentCanSee(UserPrincipal viewer, Listing listing) {
        if (viewer.getRole() == UserRole.AGENT) {
            if (listing.getAssignedAgentId() == null || !listing.getAssignedAgentId().equals(viewer.getId())) {
                throw new ResourceNotFoundException("Listing not found");
            }
        }
    }

    private void validateAssignedAgent(Long assignedAgentId) {
        if (assignedAgentId == null) {
            return;
        }
        User agent = userRepository.findById(assignedAgentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned agent not found"));
        if (agent.getRole() != UserRole.AGENT) {
            throw new IllegalArgumentException("assignedAgentId must reference a user with role AGENT");
        }
    }

    private static void applyRequest(Listing listing, ListingRequest request) {
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setOwnerName(request.getOwnerName());
        listing.setOwnerEmail(request.getOwnerEmail().trim().toLowerCase());
        listing.setAssignedAgentId(request.getAssignedAgentId());
    }
}
