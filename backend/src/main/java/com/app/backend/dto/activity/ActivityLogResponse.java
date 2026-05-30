package com.app.backend.dto.activity;

import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ActivityLogResponse {
    Long id;
    Long userId;
    String actorEmail;
    ActivityAction action;
    ActivityEntityType entityType;
    Long entityId;
    Instant occurredAt;
}
