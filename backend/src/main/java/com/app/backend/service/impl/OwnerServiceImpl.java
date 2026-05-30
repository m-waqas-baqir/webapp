package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.owners.OwnerRequest;
import com.app.backend.dto.owners.OwnerResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Owner;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.OwnerService;
import com.app.backend.service.owners.OwnerViewMapper;
import com.app.backend.support.SoftDelete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    private static final Logger log = LoggerFactory.getLogger(OwnerServiceImpl.class);

    private final OwnerRepository ownerRepository;
    private final OwnerViewMapper ownerViewMapper;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OwnerResponse> list(UserPrincipal viewer, Pageable pageable) {
        return PageResponse.from(
                ownerRepository
                        .findAll(Specification.where(SoftDeleteSpecifications.<Owner>notDeleted()), pageable)
                        .map(o -> ownerViewMapper.toResponse(o, viewer))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerResponse get(UserPrincipal viewer, Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        return ownerViewMapper.toResponse(owner, viewer);
    }

    @Override
    @Transactional
    public OwnerResponse create(UserPrincipal actor, OwnerRequest request) {
        String cnic = normalizeCnic(request.getCnic());
        if (ownerRepository.existsByCnicAndDeletedAtIsNull(cnic)) {
            throw new IllegalArgumentException("An owner with this CNIC already exists");
        }
        Owner owner = new Owner();
        applyRequest(owner, request, cnic);
        owner = ownerRepository.save(owner);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.OWNER, owner.getId());
        return ownerViewMapper.toResponse(owner, actor);
    }

    @Override
    @Transactional
    public OwnerResponse update(UserPrincipal actor, Long id, OwnerRequest request) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        String cnic = normalizeCnic(request.getCnic());
        if (ownerRepository.existsByCnicAndDeletedAtIsNullAndIdNot(cnic, id)) {
            throw new IllegalArgumentException("An owner with this CNIC already exists");
        }
        applyRequest(owner, request, cnic);
        owner = ownerRepository.save(owner);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.OWNER, owner.getId());
        return ownerViewMapper.toResponse(owner, actor);
    }

    @Override
    @Transactional
    public void delete(UserPrincipal actor, Long id) {
        Owner owner = ownerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.OWNER, id);
        SoftDelete.mark(owner);
        ownerRepository.save(owner);
        log.info("Soft-delete owner id={} by userId={}", id, actor.getId());
    }

    private static void applyRequest(Owner owner, OwnerRequest request, String normalizedCnic) {
        owner.setName(request.getName().trim());
        owner.setContactInfo(request.getContactInfo().trim());
        owner.setCnic(normalizedCnic);
    }

    private static String normalizeCnic(String cnic) {
        return cnic.trim().toUpperCase(Locale.ROOT);
    }
}
