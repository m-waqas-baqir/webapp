package com.app.backend.dto.admin;

import com.app.backend.entity.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManagedUserListItemResponse {
    Long id;
    String name;
    String email;
    UserRole role;
    boolean active;
    long permissionCount;
}
