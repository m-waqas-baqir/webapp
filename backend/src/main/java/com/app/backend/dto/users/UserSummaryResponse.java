package com.app.backend.dto.users;

import com.app.backend.entity.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSummaryResponse {
    Long id;
    String name;
    String email;
    UserRole role;
}
