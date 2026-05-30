package com.app.backend.service.excel.catalog;

import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.excel.ExcelSheetReader;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class RentalImportCatalog {

    private static String normName(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private final Set<String> ownerNamesLower;
    private final Set<String> agentNamesLower;
    private static final Set<String> TYPES = Arrays.stream(RentalPropertyType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
    private static final Set<String> STATUSES = Arrays.stream(RentalPropertyStatus.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public RentalImportCatalog(Set<String> ownerNamesLower, Set<String> agentNamesLower) {
        this.ownerNamesLower = Set.copyOf(ownerNamesLower);
        this.agentNamesLower = Set.copyOf(agentNamesLower);
    }

    public void validateFieldValues(Map<String, String> m) {
        String typeS = m.get("type");
        if (typeS != null && !typeS.isBlank()) {
            String t = typeS.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (!TYPES.contains(t)) {
                throw new IllegalArgumentException("Invalid type \"" + typeS.trim() + "\" (use RESIDENTIAL or COMMERCIAL)");
            }
        }
        String statusS = m.get("status");
        if (statusS != null && !statusS.isBlank()) {
            String st = statusS.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (!STATUSES.contains(st)) {
                throw new IllegalArgumentException("Invalid status \"" + statusS.trim() + "\" (use one of: "
                        + String.join(", ", STATUSES) + ")");
            }
        }
        String owner = m.get("ownername");
        if (owner != null && !owner.isBlank()) {
            if (!ownerNamesLower.contains(normName(owner))) {
                throw new IllegalArgumentException("Invalid ownerName \"" + owner.trim() + "\" (not in current reference data)");
            }
        }
        String agent = m.get("agentname");
        if (agent != null && !agent.isBlank()) {
            if (!agentNamesLower.contains(normName(agent))) {
                throw new IllegalArgumentException("Invalid agentName \"" + agent.trim() + "\" (not in current reference data)");
            }
        }
    }

    public static boolean isTemplateSampleRow(Map<String, String> m) {
        String t = m.get("title");
        if (t == null) {
            return false;
        }
        return "example".equals(ExcelSheetReader.normalizeHeader(t));
    }
}
