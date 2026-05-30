package com.app.backend.repository;

import com.app.backend.entity.LinkedEntityType;
import com.app.backend.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByLinkedEntityTypeAndLinkedEntityIdOrderByCreatedAtAsc(
            LinkedEntityType linkedEntityType,
            Long linkedEntityId
    );

    void deleteByLinkedEntityTypeAndLinkedEntityId(LinkedEntityType linkedEntityType, Long linkedEntityId);
}
