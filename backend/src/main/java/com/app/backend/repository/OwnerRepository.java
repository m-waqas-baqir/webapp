package com.app.backend.repository;

import com.app.backend.entity.Owner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long>, JpaSpecificationExecutor<Owner> {

    List<Owner> findByNameIgnoreCase(String name);

    boolean existsByCnicAndDeletedAtIsNull(String cnic);

    boolean existsByCnicAndDeletedAtIsNullAndIdNot(String cnic, Long id);

    Optional<Owner> findByCnicIgnoreCase(String cnic);

    Page<Owner> findByNameContainingIgnoreCase(String namePart, Pageable pageable);
}
