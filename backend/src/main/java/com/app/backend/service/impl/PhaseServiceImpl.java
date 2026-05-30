package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PhaseRequest;
import com.app.backend.dto.plots.PhaseResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Phase;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.PhaseService;
import com.app.backend.service.plots.PlotMapper;
import com.app.backend.support.SoftDelete;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhaseServiceImpl implements PhaseService {

    private static final Logger log = LoggerFactory.getLogger(PhaseServiceImpl.class);

    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final PlotMapper plotMapper;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PhaseResponse> list(Pageable pageable) {
        return PageResponse.from(
                phaseRepository
                        .findAll(Specification.where(SoftDeleteSpecifications.<Phase>notDeleted()), pageable)
                        .map(plotMapper::toPhaseResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "phasesById", key = "#id")
    public PhaseResponse get(Long id) {
        Phase phase = phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        return plotMapper.toPhaseResponse(phase);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public PhaseResponse create(UserPrincipal actor, PhaseRequest request) {
        Phase phase = new Phase();
        phase.setName(request.getName().trim());
        phase = phaseRepository.save(phase);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.PHASE, phase.getId());
        return plotMapper.toPhaseResponse(phase);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public PhaseResponse update(UserPrincipal actor, Long id, PhaseRequest request) {
        Phase phase = phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        phase.setName(request.getName().trim());
        phase = phaseRepository.save(phase);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.PHASE, phase.getId());
        return plotMapper.toPhaseResponse(phase);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public void delete(UserPrincipal actor, Long id) {
        Phase phase = phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        if (khayabanRepository.countByPhase_Id(id) > 0) {
            throw new IllegalArgumentException("Cannot delete a phase that still has khayabans");
        }
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.PHASE, id);
        SoftDelete.mark(phase);
        phaseRepository.save(phase);
        log.info("Soft-delete phase id={} by userId={}", id, actor.getId());
    }
}
