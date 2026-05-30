package com.app.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level grouping for residential/commercial sectors (e.g. Phase 1, Phase 2).
 */
@Getter
@Setter
@Entity
@Table(name = "phases")
public class Phase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @OneToMany(mappedBy = "phase", fetch = FetchType.LAZY)
    private List<Khayaban> khayabans = new ArrayList<>();
}
