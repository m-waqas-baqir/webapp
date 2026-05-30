package com.app.backend.service.rentals;

import com.app.backend.dto.rentals.RentalPropertyResponse;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.UserRole;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class RentalPropertyMapper {

    /** AGENT must not receive registry owner id/name when set. */
    public RentalPropertyResponse toResponse(RentalProperty entity, UserPrincipal viewer) {
        Long ownerLinkId = entity.getOwner() != null ? entity.getOwner().getId() : null;
        String ownerName = entity.getOwner() != null ? entity.getOwner().getName() : null;
        String agentName = entity.getAssignedAgent() != null ? entity.getAssignedAgent().getName() : null;
        if (viewer.getRole() == UserRole.AGENT && !viewer.hasPermission(PermissionCodes.OWNER_VIEW)) {
            ownerLinkId = null;
            ownerName = null;
        }
        if (viewer.getRole() == UserRole.AGENT) {
            agentName = null;
        }
        boolean canMutate = viewer.getRole() != UserRole.AGENT
                || (entity.getCreatedBy() != null && entity.getCreatedBy().getId().equals(viewer.getId()));
        return RentalPropertyResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .type(entity.getType())
                .address(entity.getAddress())
                .rentAmount(entity.getRentAmount())
                .status(entity.getStatus())
                .ownerId(ownerLinkId)
                .ownerName(ownerName)
                .assignedAgentId(entity.getAssignedAgent() != null ? entity.getAssignedAgent().getId() : null)
                .assignedAgentName(agentName)
                .canMutate(canMutate)
                .build();
    }
}
