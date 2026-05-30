package com.app.backend.repository;

import com.app.backend.entity.Plot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface PlotRepository extends JpaRepository<Plot, Long>, JpaSpecificationExecutor<Plot> {

    boolean existsByKhayaban_IdAndPlotNumberIgnoreCase(Long khayabanId, String plotNumber);

    long countByKhayaban_Id(Long khayabanId);

    Page<Plot> findByPlotNumberContainingIgnoreCase(String plotNumberPart, Pageable pageable);

    @EntityGraph(attributePaths = {"phase", "khayaban", "owner", "assignedAgent", "createdBy"})
    Page<Plot> findAll(@Nullable Specification<Plot> spec, Pageable pageable);
}
