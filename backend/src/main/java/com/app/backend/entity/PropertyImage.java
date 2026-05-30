package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Uploaded image metadata for plots and rental properties.
 * Binary bytes live on disk under {@code app.storage.root}; {@link #storagePath} is relative to that root.
 */
@Getter
@Setter
@Entity
@Table(
        name = "property_images",
        indexes = {
                @Index(name = "idx_property_images_entity", columnList = "linked_entity_type, linked_entity_id"),
        }
)
public class PropertyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "linked_entity_type", nullable = false, length = 32)
    private LinkedEntityType linkedEntityType;

    @Column(name = "linked_entity_id", nullable = false)
    private Long linkedEntityId;

    /** Original client file name (for display). */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    /** Path relative to storage root (no leading slash). */
    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
