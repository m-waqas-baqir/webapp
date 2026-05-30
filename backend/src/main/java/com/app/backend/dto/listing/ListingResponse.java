package com.app.backend.dto.listing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Role-dependent projection: AGENT never receives owner PII; ADMIN gets masked summaries only.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListingResponse {

    Long id;
    String title;
    String description;

    /** Raw owner name — DIRECTOR only. */
    String ownerName;

    /** Raw owner email — DIRECTOR only. */
    String ownerEmail;

    /**
     * Masked single-line summary for ADMIN (e.g. name initials + redacted email).
     */
    String ownerVisibilitySummary;

    Long assignedAgentId;
}
