package com.app.backend.service.excel;

import com.app.backend.entity.ActivityAction;
import com.app.backend.entity.ActivityEntityType;
import com.app.backend.entity.Owner;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.entity.User;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.UserRepository;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.ActivityLogService;
import com.app.backend.service.excel.catalog.RentalImportCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RentalImportRowService {

    private final RentalPropertyRepository rentalPropertyRepository;
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void importRow(UserPrincipal actor, Map<String, String> m, RentalImportCatalog catalog) {
        if (actor.getRole() == UserRole.AGENT) {
            throw new IllegalStateException("Bulk import is not available to AGENT");
        }
        if (catalog != null) {
            catalog.validateFieldValues(m);
        }
        String title = require(m, "title").trim();
        String typeS = require(m, "type");
        String address = require(m, "address").trim();
        String rentS = require(m, "rentamount");
        String statusS = require(m, "status");

        RentalPropertyType type = parseEnum(RentalPropertyType.class, typeS, "type");
        RentalPropertyStatus status = parseEnum(RentalPropertyStatus.class, statusS, "status");
        BigDecimal rent = parseDecimal(rentS, "rentAmount");

        User actorEntity = userRepository.findById(actor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));

        RentalProperty r = new RentalProperty();
        r.setTitle(title);
        r.setType(type);
        r.setAddress(address);
        r.setRentAmount(rent);
        r.setStatus(status);
        r.setCreatedBy(actorEntity);

        String ownerName = blankToNull(m.get("ownername"));
        if (ownerName != null) {
            r.setOwner(singleOwner(ownerName));
        }
        String agentName = blankToNull(m.get("agentname"));
        if (agentName != null) {
            r.setAssignedAgent(singleAgent(agentName));
        }

        r = rentalPropertyRepository.save(r);
        activityLogService.record(actor.getId(), ActivityAction.CREATE, ActivityEntityType.RENTAL_PROPERTY, r.getId());
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

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String label) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase().replace(' ', '_'));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + label + " value: " + raw);
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
