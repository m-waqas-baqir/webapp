package com.app.backend.dto.search;

/** One match in global search results. */
public record SearchHit(String resourceType, Long id, String title, String subtitle) {
}
