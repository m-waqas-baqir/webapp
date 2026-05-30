package com.app.backend.repository;

import com.app.backend.entity.Khayaban;
import org.springframework.data.jpa.domain.Specification;

public final class KhayabanSpecifications {

    private KhayabanSpecifications() {
    }

    public static Specification<Khayaban> phaseIdEquals(Long phaseId) {
        if (phaseId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("phase").get("id"), phaseId);
    }
}
