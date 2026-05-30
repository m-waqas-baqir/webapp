package com.app.backend.dto.admin;

import com.app.backend.entity.PermissionModule;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PermissionResponse {
    String code;
    String description;
    PermissionModule module;
}
