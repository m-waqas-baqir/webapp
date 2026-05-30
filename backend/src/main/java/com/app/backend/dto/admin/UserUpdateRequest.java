package com.app.backend.dto.admin;

import com.app.backend.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(max = 120)
    private String name;

    @Email
    @Size(max = 180)
    private String email;

    private UserRole role;

    private Boolean active;

    /** When non-null and non-blank, sets a new password. */
    @Size(min = 8, max = 120)
    private String newPassword;
}
