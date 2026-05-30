package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Individual plot unit: sized, priced, and linked to phase/khayaban and optional owner/agent.
 * DB indexes support phase/khayaban/status filters and numeric range queries on price/size;
 * in PostgreSQL consider covering indexes (INCLUDE) for heavy dashboards.
 */
@Getter
@Setter
@Entity
@Table(
        name = "plots",
        uniqueConstraints = @UniqueConstraint(name = "uk_plot_khayaban_number", columnNames = {"khayaban_id", "plot_number"}),
        indexes = {
                @Index(name = "idx_plots_phase_id", columnList = "phase_id"),
                @Index(name = "idx_plots_khayaban_id", columnList = "khayaban_id"),
                @Index(name = "idx_plots_status", columnList = "status"),
                @Index(name = "idx_plots_price", columnList = "price"),
                @Index(name = "idx_plots_size", columnList = "size"),
        }
)
public class Plot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plot_number", nullable = false, length = 64)
    private String plotNumber;

    /** Plot size (e.g. marla/sq.ft.) — application defines unit. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal size;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PlotStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khayaban_id", nullable = false)
    private Khayaban khayaban;

    /** Registry {@link Owner} (not an app login user). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    /** User who created this plot (for AGENT update ownership). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
