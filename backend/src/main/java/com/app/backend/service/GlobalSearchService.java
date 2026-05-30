package com.app.backend.service;

import com.app.backend.dto.search.GlobalSearchResponse;
import com.app.backend.security.UserPrincipal;

public interface GlobalSearchService {

    GlobalSearchResponse search(UserPrincipal viewer, String query, int limitPerType);
}
