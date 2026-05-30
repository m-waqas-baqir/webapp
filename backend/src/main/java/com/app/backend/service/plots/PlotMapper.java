package com.app.backend.service.plots;

import com.app.backend.dto.plots.KhayabanResponse;
import com.app.backend.dto.plots.PhaseResponse;
import com.app.backend.dto.plots.PlotResponse;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.UserRole;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import org.springframework.stereotype.Component;

@Component
public class PlotMapper {

    public PhaseResponse toPhaseResponse(Phase phase) {
        return PhaseResponse.builder()
                .id(phase.getId())
                .name(phase.getName())
                .build();
    }

    public KhayabanResponse toKhayabanResponse(Khayaban khayaban) {
        return KhayabanResponse.builder()
                .id(khayaban.getId())
                .name(khayaban.getName())
                .phaseId(khayaban.getPhase().getId())
                .build();
    }

    /**
     * Role-aware projection: AGENT must not receive registry owner id/name (enumeration risk).
     * DIRECTOR and ADMIN retain operational linkage ids and display names.
     */
    public PlotResponse toPlotResponse(Plot plot, UserPrincipal viewer) {
        Long ownerLinkId = plot.getOwner() != null ? plot.getOwner().getId() : null;
        String ownerName = plot.getOwner() != null ? plot.getOwner().getName() : null;
        String agentName = plot.getAssignedAgent() != null ? plot.getAssignedAgent().getName() : null;
        if (viewer.getRole() == UserRole.AGENT && !viewer.hasPermission(PermissionCodes.OWNER_VIEW)) {
            ownerLinkId = null;
            ownerName = null;
        }
        if (viewer.getRole() == UserRole.AGENT) {
            agentName = null;
        }
        boolean canMutate = viewer.getRole() != UserRole.AGENT
                || (plot.getCreatedBy() != null && plot.getCreatedBy().getId().equals(viewer.getId()));
        return PlotResponse.builder()
                .id(plot.getId())
                .plotNumber(plot.getPlotNumber())
                .size(plot.getSize())
                .price(plot.getPrice())
                .status(plot.getStatus())
                .phaseId(plot.getPhase().getId())
                .khayabanId(plot.getKhayaban().getId())
                .ownerId(ownerLinkId)
                .ownerName(ownerName)
                .assignedAgentId(plot.getAssignedAgent() != null ? plot.getAssignedAgent().getId() : null)
                .assignedAgentName(agentName)
                .canMutate(canMutate)
                .build();
    }
}
