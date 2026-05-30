package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Sample domain object for RBAC: listings carry owner PII and optional agent assignment.
 */
@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    /** Owner display name — treated as sensitive for AGENT views. */
    @Column(nullable = false, length = 200)
    private String ownerName;

    /** Owner contact — only exposed to DIRECTOR in API responses. */
    @Column(nullable = false, length = 200)
    private String ownerEmail;

    /** When set, only this user (AGENT) should see the listing in scoped queries. */
    @Column(name = "assigned_agent_id")
    private Long assignedAgentId;
}
