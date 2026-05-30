package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Street or block within a {@link Phase}.
 */
@Getter
@Setter
@Entity
@Table(
        name = "khayabans",
        uniqueConstraints = @UniqueConstraint(name = "uk_khayaban_phase_name", columnNames = {"phase_id", "name"})
)
public class Khayaban extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @OneToMany(mappedBy = "khayaban", fetch = FetchType.LAZY)
    private List<Plot> plots = new ArrayList<>();
}
