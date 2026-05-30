package com.app.backend.service.excel;

import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.excel.ExcelEnterpriseTemplateFactory;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelTemplateGeneratorService {

    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;

    public byte[] plotTemplate() throws IOException {
        List<Phase> phases = phaseRepository.findAll(Sort.by("name"));
        List<String> phaseNames = phases.stream().map(Phase::getName).toList();

        List<String> khayabanNames = new ArrayList<>();
        String exampleKhayaban = "";
        for (Phase p : phases) {
            List<Khayaban> ks = khayabanRepository.findAllByPhase_IdOrderByNameAsc(p.getId());
            for (Khayaban k : ks) {
                khayabanNames.add(k.getName());
                if (exampleKhayaban.isEmpty()) {
                    exampleKhayaban = k.getName();
                }
            }
        }

        List<String> ownerNames = ownerRepository.findAll().stream()
                .sorted(Comparator.comparing(Owner::getName, String.CASE_INSENSITIVE_ORDER))
                .map(Owner::getName)
                .toList();

        List<String> agentNames = userRepository.findByRoleOrderByNameAsc(UserRole.AGENT).stream()
                .map(User::getName)
                .toList();

        return ExcelEnterpriseTemplateFactory.plotTemplate(
                phaseNames, khayabanNames, ownerNames, agentNames, exampleKhayaban);
    }

    public byte[] khayabanTemplate() throws IOException {
        List<String> phaseNames = phaseRepository.findAll(Sort.by("name")).stream()
                .map(Phase::getName)
                .toList();
        return ExcelEnterpriseTemplateFactory.khayabanTemplate(phaseNames);
    }

    public byte[] rentalTemplate() throws IOException {
        List<String> ownerNames = ownerRepository.findAll().stream()
                .sorted(Comparator.comparing(Owner::getName, String.CASE_INSENSITIVE_ORDER))
                .map(Owner::getName)
                .toList();
        List<String> agentNames = userRepository.findByRoleOrderByNameAsc(UserRole.AGENT).stream()
                .map(User::getName)
                .toList();
        return ExcelEnterpriseTemplateFactory.rentalTemplate(ownerNames, agentNames);
    }
}
