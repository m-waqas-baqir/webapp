package com.app.backend.repository;

import com.app.backend.entity.RentalProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalPropertyRepository extends JpaRepository<RentalProperty, Long>, JpaSpecificationExecutor<RentalProperty> {

    Page<RentalProperty> findByTitleContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String titlePart,
            String addressPart,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"owner", "assignedAgent", "createdBy"})
    Page<RentalProperty> findAll(@Nullable Specification<RentalProperty> spec, Pageable pageable);
}
