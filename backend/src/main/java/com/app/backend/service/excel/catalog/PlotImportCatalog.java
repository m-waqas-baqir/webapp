package com.app.backend.service.excel.catalog;

import com.app.backend.entity.PlotStatus;
import com.app.backend.excel.ExcelSheetReader;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pre-validates plot import rows against known reference data (phases, khayabans, etc.)
 * to produce clearer errors before DB lookups.
 */
public final class PlotImportCatalog {

    private static String normName(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private final Set<String> phaseNamesLower;
    private final Map<String, Set<String>> khayabansByPhaseLower;
    private final Set<String> ownerNamesLower;
    private final Set<String> agentNamesLower;
    private static final Set<String> STATUS_VALUES = Arrays.stream(PlotStatus.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public PlotImportCatalog(
            Set<String> phaseNamesLower,
            Map<String, Set<String>> khayabansByPhaseLower,
            Set<String> ownerNamesLower,
            Set<String> agentNamesLower) {
        this.phaseNamesLower = Set.copyOf(phaseNamesLower);
        this.khayabansByPhaseLower = Map.copyOf(khayabansByPhaseLower);
        this.ownerNamesLower = Set.copyOf(ownerNamesLower);
        this.agentNamesLower = Set.copyOf(agentNamesLower);
    }

    public void validateFieldValues(Map<String, String> m) {
        String phase = m.get("phasename");
        if (phase != null && !phase.isBlank()) {
            String pKey = normName(phase);
            if (!phaseNamesLower.contains(pKey)) {
                throw new IllegalArgumentException("Invalid phaseName \"" + phase.trim() + "\" (not in current reference data)");
            }
        }
        String k = m.get("khayabanname");
        if (k != null && !k.isBlank() && phase != null && !phase.isBlank()) {
            String pKey = normName(phase);
            String kk = normName(k);
            Set<String> set = khayabansByPhaseLower.get(pKey);
            if (set == null || !set.contains(kk)) {
                throw new IllegalArgumentException("Invalid khayabanName \"" + k.trim() + "\" for phase \""
                        + phase.trim() + "\" (not a valid pair in reference data)");
            }
        }
        String statusS = m.get("status");
        if (statusS != null && !statusS.isBlank()) {
            String st = statusS.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (!STATUS_VALUES.contains(st)) {
                throw new IllegalArgumentException("Invalid status \"" + statusS.trim() + "\" (use one of: "
                        + String.join(", ", STATUS_VALUES) + ")");
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
        String pn = m.get("plotnumber");
        if (pn == null) {
            return false;
        }
        return "example".equals(ExcelSheetReader.normalizeHeader(pn));
    }
}
