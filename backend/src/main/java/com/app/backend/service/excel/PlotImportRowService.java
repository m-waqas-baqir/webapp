package com.app.backend.service.excel;

import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.PlotStatus;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.excel.catalog.PlotImportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One plot per transaction so bulk import can partially succeed.
 */
@Service
@RequiredArgsConstructor
public class PlotImportRowService {

    private final PlotRepository plotRepository;
    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void importRow(UserPrincipal actor, Map<String, String> m, PlotImportCatalog catalog) {
        if (actor.getRole() == UserRole.AGENT) {
            throw new IllegalStateException("Bulk import is not available to AGENT");
        }
        if (catalog != null) {
            catalog.validateFieldValues(m);
        }
        String plotNumber = require(m, "plotnumber");
        String sizeS = require(m, "size");
        String priceS = require(m, "price");
        String statusS = require(m, "status");
        String phaseName = require(m, "phasename");
        String khayabanName = require(m, "khayabanname");

        Phase phase = singlePhase(phaseName.trim());
        Khayaban khayaban = khayabanRepository
                .findByPhase_IdAndNameIgnoreCase(phase.getId(), khayabanName.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khayaban '" + khayabanName + "' not found under phase '" + phaseName + "'"));
        if (!khayaban.getPhase().getId().equals(phase.getId())) {
            throw new IllegalArgumentException("Khayaban does not belong to the given phase");
        }

        String pn = plotNumber.trim();
        if (plotRepository.existsByKhayaban_IdAndPlotNumberIgnoreCase(khayaban.getId(), pn)) {
            throw new IllegalArgumentException("Plot number already exists in this khayaban");
        }

        PlotStatus status = parsePlotStatus(statusS);
        BigDecimal size = parseDecimal(sizeS, "size");
        BigDecimal price = parseDecimal(priceS, "price");

        User actorEntity = userRepository.findById(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        Plot plot = new Plot();
        plot.setPlotNumber(pn);
        plot.setSize(size);
        plot.setPrice(price);
        plot.setStatus(status);
        plot.setPhase(phase);
        plot.setKhayaban(khayaban);
        plot.setCreatedBy(actorEntity);

        String ownerName = blankToNull(m.get("ownername"));
        if (ownerName != null) {
            plot.setOwner(singleOwner(ownerName));
        }
        String agentName = blankToNull(m.get("agentname"));
        if (agentName != null) {
            plot.setAssignedAgent(singleAgent(agentName));
        }

        plot = plotRepository.save(plot);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.PLOT, plot.getId());
    }

    private static String require(Map<String, String> m, String key) {
        String v = m.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing value for column: " + key);
        }
        return v;
    }

    private static String blankToNull(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim();
    }

    private Phase singlePhase(String name) {
        List<Phase> list = phaseRepository.findByNameIgnoreCase(name);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Phase not found: " + name);
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("Ambiguous phase name (multiple matches): " + name);
        }
        return list.get(0);
    }

    private Owner singleOwner(String name) {
        List<Owner> list = ownerRepository.findByNameIgnoreCase(name);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Owner not found: " + name);
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("Ambiguous owner name (multiple matches): " + name);
        }
        return list.get(0);
    }

    private User singleAgent(String name) {
        List<User> list = userRepository.findByNameIgnoreCaseAndRole(name, UserRole.AGENT);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Agent not found (must be user with role AGENT): " + name);
        }
        if (list.size() > 1) {
            throw new IllegalArgumentException("Ambiguous agent name (multiple matches): " + name);
        }
        return list.get(0);
    }

    private static PlotStatus parsePlotStatus(String raw) {
        try {
            return PlotStatus.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid plot status (use AVAILABLE, RESERVED, SOLD, HOLD): " + raw);
        }
    }

    private static BigDecimal parseDecimal(String raw, String label) {
        try {
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + label + " number: " + raw);
        }
    }
}
