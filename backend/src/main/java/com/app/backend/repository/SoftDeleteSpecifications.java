package com.app.backend.repository;

import com.app.backend.entity.BaseEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Explicit {@code deleted_at IS NULL} filter for JPA criteria queries.
 * Complements Hibernate {@code @SQLRestriction} on {@link BaseEntity} (defense in depth for
 * Spring Data derived queries and specification-based lists).
 */
public final class SoftDeleteSpecifications {

    private SoftDeleteSpecifications() {
    }

    public static <T extends BaseEntity> Specification<T> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
