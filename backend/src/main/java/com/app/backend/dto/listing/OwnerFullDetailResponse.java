package com.app.backend.dto.listing;

import lombok.Builder;
import lombok.Value;

/**
 * Director-only owner visibility across listings.
 */
@Value
@Builder
public class OwnerFullDetailResponse {

    Long listingId;
    String listingTitle;
    String ownerName;
    String ownerEmail;
    Long assignedAgentId;
}
