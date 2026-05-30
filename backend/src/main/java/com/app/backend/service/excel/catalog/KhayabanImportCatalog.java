package com.app.backend.service.excel.catalog;

import com.app.backend.excel.ExcelSheetReader;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class KhayabanImportCatalog {

    private static String normName(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private final Set<String> phaseNamesLower;

    public KhayabanImportCatalog(Set<String> phaseNamesLower) {
        this.phaseNamesLower = Set.copyOf(phaseNamesLower);
    }

    public void validateFieldValues(Map<String, String> m) {
        String phase = m.get("phasename");
        if (phase != null && !phase.isBlank()) {
            if (!phaseNamesLower.contains(normName(phase))) {
                throw new IllegalArgumentException("Invalid phaseName \"" + phase.trim() + "\" (not in current reference data)");
            }
        }
    }

    public static boolean isTemplateSampleRow(Map<String, String> m) {
        String n = m.get("name");
        if (n == null) {
            return false;
        }
        return "example".equals(ExcelSheetReader.normalizeHeader(n));
    }
}
