package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Property owner identity document — sensitive fields masked per role in API responses.
 */
@Getter
@Setter
@Entity
@Table(
        name = "owners",
        uniqueConstraints = @UniqueConstraint(name = "uk_owners_cnic", columnNames = "cnic")
)
public class Owner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    /** Phone, email, or combined contact line — treated as sensitive. */
    @Column(nullable = false, length = 500)
    private String contactInfo;

    /** National ID (e.g. CNIC) — stored in full; never exposed raw except to DIRECTOR. */
    @Column(nullable = false, length = 20)
    private String cnic;
}
