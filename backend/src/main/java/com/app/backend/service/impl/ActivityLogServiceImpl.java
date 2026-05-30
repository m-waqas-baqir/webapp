package com.app.backend.service.impl;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.activity.ActivityLogResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.ActivityLog;
import com.app.backend.entity.User;
import com.app.backend.repository.ActivityLogRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void record(Long userId, ActivityAction action, ActivityEntityType entityType, Long entityId) {
        ActivityLog log = new ActivityLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        activityLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> list(Pageable pageable) {
        return PageResponse.from(activityLogRepository.findAllByOrderByOccurredAtDesc(pageable).map(this::toResponse));
    }

    private ActivityLogResponse toResponse(ActivityLog e) {
        String email = userRepository.findById(e.getUserId()).map(User::getEmail).orElse(null);
        return ActivityLogResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .actorEmail(email)
                .action(e.getAction())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .occurredAt(e.getOccurredAt())
                .build();
    }
}
