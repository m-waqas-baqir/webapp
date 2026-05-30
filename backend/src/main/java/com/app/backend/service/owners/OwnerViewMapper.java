package com.app.backend.service.owners;

import com.app.backend.dto.owners.OwnerResponse;
import com.app.backend.entity.Owner;
import com.app.backend.entity.UserRole;
import com.app.backend.security.PermissionCodes;
import com.app.backend.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class OwnerViewMapper {

    public OwnerResponse toResponse(Owner owner, UserPrincipal viewer) {
        return switch (viewer.getRole()) {
            case DIRECTOR -> OwnerResponse.builder()
                    .id(owner.getId())
                    .name(owner.getName())
                    .contactInfo(owner.getContactInfo())
                    .cnic(owner.getCnic())
                    .build();
            case ADMIN -> OwnerResponse.builder()
                    .id(owner.getId())
                    .name(maskName(owner.getName()))
                    .contactInfo(maskContactInfo(owner.getContactInfo()))
                    .cnic(maskCnic(owner.getCnic()))
                    .build();
            case AGENT -> {
                if (!viewer.hasPermission(PermissionCodes.OWNER_VIEW)) {
                    yield OwnerResponse.builder()
                            .id(owner.getId())
                            .name(maskNameAggressive(owner.getName()))
                            .build();
                }
                yield OwnerResponse.builder()
                        .id(owner.getId())
                        .name(maskName(owner.getName()))
                        .contactInfo(maskContactInfo(owner.getContactInfo()))
                        .cnic(maskCnic(owner.getCnic()))
                        .build();
            }
        };
    }

    /** ADMIN — same style as listing owner summary: initials-style name. */
    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "—";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            String p = parts[0];
            return p.charAt(0) + "***";
        }
        return parts[0].charAt(0) + ". " + parts[parts.length - 1].charAt(0) + ".";
    }

    /** AGENT — owner identity minimally disclosed. */
    private static String maskNameAggressive(String name) {
        if (name == null || name.isBlank()) {
            return "—";
        }
        return name.trim().charAt(0) + "***";
    }

    private static String maskContactInfo(String contact) {
        if (contact == null || contact.isBlank()) {
            return "—";
        }
        String t = contact.trim();
        if (t.contains("@")) {
            return maskEmail(t);
        }
        return maskPhoneDigits(t);
    }

    private static String maskEmail(String email) {
        if (!email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at + 1);
        String localMasked = local.length() <= 1 ? "*" : local.charAt(0) + "***";
        int dot = domain.lastIndexOf('.');
        String domainMasked = dot > 0
                ? domain.charAt(0) + "***." + domain.substring(dot + 1).toLowerCase(Locale.ROOT)
                : "***";
        return localMasked + "@" + domainMasked;
    }

    /** Keep last 4 digits if present; otherwise redact. */
    private static String maskPhoneDigits(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "•••• " + digits.substring(digits.length() - 4);
    }

    /** Last 4 characters visible for ADMIN (CNIC often includes dashes). */
    private static String maskCnic(String cnic) {
        if (cnic == null || cnic.isBlank()) {
            return "—";
        }
        String t = cnic.trim();
        if (t.length() <= 4) {
            return "****";
        }
        return "************" + t.substring(t.length() - 4);
    }
}
