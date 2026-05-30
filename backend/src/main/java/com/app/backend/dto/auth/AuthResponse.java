package com.app.backend.dto.auth;

import com.app.backend.entity.UserRole;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AuthResponse {

    String accessToken;
    String tokenType;
    long expiresInSeconds;
    Long userId;
    String email;
    String name;
    UserRole role;
    boolean active;
    @Singular("permission")
    List<String> permissions;
}
