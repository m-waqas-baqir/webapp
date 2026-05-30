package com.app.backend.dto.search;

import java.util.List;

public record GlobalSearchResponse(String query, int limitPerType, List<SearchHit> hits, int totalHits) {
}
