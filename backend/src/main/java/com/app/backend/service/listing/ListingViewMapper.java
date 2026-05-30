package com.app.backend.service.listing;

import com.app.backend.dto.listing.ListingResponse;
import com.app.backend.dto.listing.OwnerFullDetailResponse;
import com.app.backend.entity.Listing;
import com.app.backend.entity.UserRole;
import com.app.backend.security.UserPrincipal;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ListingViewMapper {

    public ListingResponse toResponse(Listing listing, UserPrincipal viewer) {
        return switch (viewer.getRole()) {
            case DIRECTOR -> ListingResponse.builder()
                    .id(listing.getId())
                    .title(listing.getTitle())
                    .description(listing.getDescription())
                    .ownerName(listing.getOwnerName())
                    .ownerEmail(listing.getOwnerEmail())
                    .assignedAgentId(listing.getAssignedAgentId())
                    .build();
            case ADMIN -> ListingResponse.builder()
                    .id(listing.getId())
                    .title(listing.getTitle())
                    .description(listing.getDescription())
                    .ownerVisibilitySummary(buildAdminSummary(listing))
                    .assignedAgentId(listing.getAssignedAgentId())
                    .build();
            case AGENT -> ListingResponse.builder()
                    .id(listing.getId())
                    .title(listing.getTitle())
                    .description(listing.getDescription())
                    .assignedAgentId(listing.getAssignedAgentId())
                    .build();
        };
    }

    public OwnerFullDetailResponse toDirectorRow(Listing listing) {
        return OwnerFullDetailResponse.builder()
                .listingId(listing.getId())
                .listingTitle(listing.getTitle())
                .ownerName(listing.getOwnerName())
                .ownerEmail(listing.getOwnerEmail())
                .assignedAgentId(listing.getAssignedAgentId())
                .build();
    }

    private static String buildAdminSummary(Listing listing) {
        String maskedName = maskName(listing.getOwnerName());
        String maskedEmail = maskEmail(listing.getOwnerEmail());
        return maskedName + " • " + maskedEmail;
    }

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

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
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
}
