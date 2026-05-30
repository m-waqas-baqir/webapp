package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Immutable-style audit trail for mutating HTTP calls (not soft-deleted).
 */
@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_email", length = 180)
    private String actorEmail;

    @Column(nullable = false, length = 12)
    private String httpMethod;

    @Column(nullable = false, length = 512)
    private String requestPath;

    private Integer statusCode;

    @Column(length = 45)
    private String clientIp;
}
