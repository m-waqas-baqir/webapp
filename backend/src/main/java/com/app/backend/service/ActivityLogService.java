package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.activity.ActivityLogResponse;
import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import org.springframework.data.domain.Pageable;

public interface ActivityLogService {

    void record(Long userId, ActivityAction action, ActivityEntityType entityType, Long entityId);

    PageResponse<ActivityLogResponse> list(Pageable pageable);
}
