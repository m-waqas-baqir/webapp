package com.app.backend.service;

import com.app.backend.dto.listing.ListingRequest;
import com.app.backend.dto.listing.ListingResponse;
import com.app.backend.dto.listing.OwnerFullDetailResponse;
import com.app.backend.security.UserPrincipal;

import java.util.List;

public interface ListingService {

    List<ListingResponse> listFor(UserPrincipal viewer);

    ListingResponse getFor(UserPrincipal viewer, Long id);

    ListingResponse create(UserPrincipal actor, ListingRequest request);

    ListingResponse update(UserPrincipal actor, Long id, ListingRequest request);

    void delete(UserPrincipal actor, Long id);

    /** DIRECTOR: full owner PII across all listings. */
    List<OwnerFullDetailResponse> listOwnerDetailsForDirector();
}
