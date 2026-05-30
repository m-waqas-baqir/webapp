package com.app.backend.dto.admin;

import com.app.backend.entity.UserRole;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class ManagedUserDetailResponse {
    Long id;
    String name;
    String email;
    UserRole role;
    boolean active;
    Set<String> permissions;
}
