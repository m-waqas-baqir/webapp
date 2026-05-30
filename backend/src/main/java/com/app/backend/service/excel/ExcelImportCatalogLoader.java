package com.app.backend.service.excel;

import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.service.excel.catalog.KhayabanImportCatalog;
import com.app.backend.service.excel.catalog.PlotImportCatalog;
import com.app.backend.service.excel.catalog.RentalImportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExcelImportCatalogLoader {

    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;

    private static String nl(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public PlotImportCatalog loadPlotCatalog() {
        List<Phase> phases = phaseRepository.findAll(Sort.by("name"));
        Set<String> phaseLower = new HashSet<>();
        Map<String, Set<String>> kBy = new HashMap<>();
        for (Phase p : phases) {
            String pk = nl(p.getName());
            phaseLower.add(pk);
            Set<String> ks = new HashSet<>();
            List<Khayaban> list = khayabanRepository.findAllByPhase_IdOrderByNameAsc(p.getId());
            for (Khayaban k : list) {
                ks.add(nl(k.getName()));
            }
            kBy.put(pk, ks);
        }
        Set<String> owners = new HashSet<>();
        for (Owner o : ownerRepository.findAll()) {
            owners.add(nl(o.getName()));
        }
        Set<String> agents = new HashSet<>();
        for (User u : userRepository.findByRoleOrderByNameAsc(UserRole.AGENT)) {
            agents.add(nl(u.getName()));
        }
        return new PlotImportCatalog(phaseLower, kBy, owners, agents);
    }

    public KhayabanImportCatalog loadKhayabanCatalog() {
        Set<String> phaseLower = new HashSet<>();
        for (Phase p : phaseRepository.findAll(Sort.by("name"))) {
            phaseLower.add(nl(p.getName()));
        }
        return new KhayabanImportCatalog(phaseLower);
    }

    public RentalImportCatalog loadRentalCatalog() {
        Set<String> owners = new HashSet<>();
        for (Owner o : ownerRepository.findAll()) {
            owners.add(nl(o.getName()));
        }
        Set<String> agents = new HashSet<>();
        for (User u : userRepository.findByRoleOrderByNameAsc(UserRole.AGENT)) {
            agents.add(nl(u.getName()));
        }
        return new RentalImportCatalog(owners, agents);
    }
}
