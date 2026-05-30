package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.KhayabanRequest;
import com.app.backend.dto.plots.KhayabanResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Phase;
import com.app.backend.exception.ResourceNotFoundException;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.KhayabanSpecifications;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.repository.PlotRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.KhayabanService;
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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KhayabanServiceImpl implements KhayabanService {

    private static final Logger log = LoggerFactory.getLogger(KhayabanServiceImpl.class);

    private final KhayabanRepository khayabanRepository;
    private final PhaseRepository phaseRepository;
    private final PlotRepository plotRepository;
    private final PlotMapper plotMapper;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<KhayabanResponse> list(Optional<Long> phaseId, Pageable pageable) {
        Specification<Khayaban> spec = Specification.where(KhayabanSpecifications.phaseIdEquals(phaseId.orElse(null)));
        return PageResponse.from(khayabanRepository.findAll(spec, pageable).map(plotMapper::toKhayabanResponse));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "khayabansById", key = "#id")
    public KhayabanResponse get(Long id) {
        Khayaban khayaban = khayabanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        return plotMapper.toKhayabanResponse(khayaban);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public KhayabanResponse create(UserPrincipal actor, KhayabanRequest request) {
        Phase phase = phaseRepository.findById(request.getPhaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        Khayaban khayaban = new Khayaban();
        khayaban.setName(request.getName().trim());
        khayaban.setPhase(phase);
        khayaban = khayabanRepository.save(khayaban);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.KHAYABAN, khayaban.getId());
        return plotMapper.toKhayabanResponse(khayaban);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public KhayabanResponse update(UserPrincipal actor, Long id, KhayabanRequest request) {
        Khayaban khayaban = khayabanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        Phase phase = phaseRepository.findById(request.getPhaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found"));
        khayaban.setName(request.getName().trim());
        khayaban.setPhase(phase);
        khayaban = khayabanRepository.save(khayaban);
        activityLogService.record(actor.getId(), ActivityAction.UPDATE, ActivityEntityType.KHAYABAN, khayaban.getId());
        return plotMapper.toKhayabanResponse(khayaban);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"phasesById", "khayabansById"}, allEntries = true)
    public void delete(UserPrincipal actor, Long id) {
        Khayaban khayaban = khayabanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khayaban not found"));
        if (plotRepository.countByKhayaban_Id(id) > 0) {
            throw new IllegalArgumentException("Cannot delete a khayaban that still has plots");
        }
        activityLogService.record(actor.getId(), ActivityAction.DELETE, ActivityEntityType.KHAYABAN, id);
        SoftDelete.mark(khayaban);
        khayabanRepository.save(khayaban);
        log.info("Soft-delete khayaban id={} by userId={}", id, actor.getId());
    }
}
