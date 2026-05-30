package com.app.backend.repository;

import com.app.backend.entity.Khayaban;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.util.List;

@Repository
public interface KhayabanRepository extends JpaRepository<Khayaban, Long>, JpaSpecificationExecutor<Khayaban> {

    long countByPhase_Id(Long phaseId);

    List<Khayaban> findAllByPhase_IdOrderByNameAsc(Long phaseId);

    Page<Khayaban> findByNameContainingIgnoreCase(String namePart, Pageable pageable);

    Optional<Khayaban> findByPhase_IdAndNameIgnoreCase(Long phaseId, String name);

    boolean existsByPhase_IdAndNameIgnoreCase(Long phaseId, String name);

    @EntityGraph(attributePaths = {"phase"})
    Page<Khayaban> findAll(@Nullable Specification<Khayaban> spec, Pageable pageable);
}
