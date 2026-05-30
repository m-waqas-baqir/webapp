package com.app.backend.dto.auth;

import com.app.backend.entity.UserRole;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserResponse {

    Long id;
    String name;
    String email;
    UserRole role;
    boolean active;
    @Singular("permission")
    List<String> permissions;
}
