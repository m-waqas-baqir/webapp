package com.app.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class UserPermissionsRequest {

    @NotNull
    private Set<String> permissionCodes;
}
