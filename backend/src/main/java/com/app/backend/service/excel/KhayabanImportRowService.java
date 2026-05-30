package com.app.backend.service.excel;

import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Phase;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.excel.catalog.KhayabanImportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KhayabanImportRowService {

    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final ActivityLogService activityLogService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void importRow(UserPrincipal actor, Map<String, String> m, KhayabanImportCatalog catalog) {
        if (actor.getRole() == UserRole.AGENT) {
            throw new IllegalStateException("Bulk import is not available to AGENT");
        }
        if (catalog != null) {
            catalog.validateFieldValues(m);
        }
        String name = require(m, "name").trim();
        String phaseName = require(m, "phasename").trim();

        Phase phase = singlePhase(phaseName);
        if (khayabanRepository.existsByPhase_IdAndNameIgnoreCase(phase.getId(), name)) {
            throw new IllegalArgumentException("Khayaban already exists in this phase");
        }

        Khayaban k = new Khayaban();
        k.setName(name);
        k.setPhase(phase);
        k = khayabanRepository.save(k);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.KHAYABAN, k.getId());
    }

    private static String require(Map<String, String> m, String key) {
        String v = m.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing value for column: " + key);
        }
        return v;
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
}
