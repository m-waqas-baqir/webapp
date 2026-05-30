package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Adds soft-delete to {@link AuditedEntity}. Rows with {@code deletedAt != null} are excluded from normal loads.
 */
@Getter
@Setter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseEntity extends AuditedEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
