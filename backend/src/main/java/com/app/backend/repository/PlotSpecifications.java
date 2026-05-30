package com.app.backend.repository;

import com.app.backend.entity.Plot;
import com.app.backend.entity.PlotStatus;
import com.app.backend.entity.UserRole;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class PlotSpecifications {

    private PlotSpecifications() {
    }

    /** DIRECTOR/ADMIN: no row restriction; AGENT: assigned to them OR created by them. */
    public static Specification<Plot> visibleTo(UserPrincipal principal) {
        if (principal.getRole() == UserRole.AGENT) {
            return (root, query, cb) -> {
                var assigned = cb.equal(root.get("assignedAgent").get("id"), principal.getId());
                var created = cb.and(
                        cb.isNotNull(root.get("createdBy")),
                        cb.equal(root.get("createdBy").get("id"), principal.getId())
                );
                return cb.or(assigned, created);
            };
        }
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Plot> phaseIdEquals(Long phaseId) {
        if (phaseId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("phase").get("id"), phaseId);
    }

    public static Specification<Plot> khayabanIdEquals(Long khayabanId) {
        if (khayabanId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("khayaban").get("id"), khayabanId);
    }

    public static Specification<Plot> statusEquals(PlotStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Inclusive price range; omit bounds with {@code null}. */
    public static Specification<Plot> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            if (minPrice != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return p;
        };
    }

    /** Inclusive size range; omit bounds with {@code null}. */
    public static Specification<Plot> sizeBetween(BigDecimal minSize, BigDecimal maxSize) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            if (minSize != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("size"), minSize));
            }
            if (maxSize != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("size"), maxSize));
            }
            return p;
        };
    }
}
