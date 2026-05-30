package com.app.backend.repository;

import com.app.backend.entity.Phase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, Long>, JpaSpecificationExecutor<Phase> {

    Page<Phase> findByNameContainingIgnoreCase(String namePart, Pageable pageable);

    List<Phase> findByNameIgnoreCase(String name);
}
