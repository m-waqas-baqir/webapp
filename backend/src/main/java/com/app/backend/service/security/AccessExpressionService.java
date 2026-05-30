package com.app.backend.service.security;

import com.app.backend.entity.UserRole;
import com.app.backend.repository.PlotRepository;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * SpEL-backed access checks ({@code @PreAuthorize("@access....")}). Backend remains authoritative.
 */
@Service("access")
@RequiredArgsConstructor
public class AccessExpressionService {

    private final PlotRepository plotRepository;

    private static UserPrincipal principal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal up)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return up;
    }

    /** Owner registry read: DIRECTOR / ADMIN baseline, or AGENT with OWNER_VIEW. */
    public boolean canViewOwnerRegistry(Authentication authentication) {
        UserPrincipal p = principal(authentication);
        if (p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN) {
            return true;
        }
        return p.getRole() == UserRole.AGENT && p.hasPermission(PermissionCodes.OWNER_VIEW);
    }

    /** Owner registry mutations (create/update/delete owner rows). */
    public boolean canMutateOwnerRegistry(Authentication authentication) {
        UserPrincipal p = principal(authentication);
        if (p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN) {
            return true;
        }
        return p.getRole() == UserRole.AGENT && p.hasPermission(PermissionCodes.OWNER_EDIT);
    }

    public boolean canDeletePlot(Authentication authentication, Long plotId) {
        UserPrincipal p = principal(authentication);
        if (p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN) {
            return true;
        }
        if (p.getRole() != UserRole.AGENT) {
            return false;
        }
        if (!p.hasPermission(PermissionCodes.PLOT_DELETE)) {
            return false;
        }
        return plotRepository.findById(plotId)
                .map(plot -> plot.getCreatedBy() != null && plot.getCreatedBy().getId().equals(p.getId()))
                .orElse(false);
    }

    public boolean canViewActivityLogs(Authentication authentication) {
        UserPrincipal p = principal(authentication);
        if (p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN) {
            return true;
        }
        return p.getRole() == UserRole.AGENT && p.hasPermission(PermissionCodes.VIEW_ACTIVITY_LOGS);
    }

    /** User management console (list/create/update/deactivate users). */
    public boolean canAccessUserManagement(Authentication authentication) {
        UserPrincipal p = principal(authentication);
        return p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN;
    }

    /** Assign or replace arbitrary permission grants (restricted inside service for ADMIN). */
    public boolean canAssignPermissions(Authentication authentication) {
        UserPrincipal p = principal(authentication);
        return p.getRole() == UserRole.DIRECTOR || p.getRole() == UserRole.ADMIN;
    }
}
