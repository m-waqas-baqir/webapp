package com.app.backend.repository;

import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.entity.UserRole;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class RentalPropertySpecifications {

    private RentalPropertySpecifications() {
    }

    /** DIRECTOR/ADMIN: all rows; AGENT: assigned to them OR created by them. */
    public static Specification<RentalProperty> visibleTo(UserPrincipal principal) {
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

    public static Specification<RentalProperty> typeEquals(RentalPropertyType type) {
        if (type == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<RentalProperty> statusEquals(RentalPropertyStatus status) {
        if (status == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Inclusive rent amount range. */
    public static Specification<RentalProperty> rentAmountBetween(BigDecimal minRent, BigDecimal maxRent) {
        return (root, query, cb) -> {
            var p = cb.conjunction();
            if (minRent != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("rentAmount"), minRent));
            }
            if (maxRent != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("rentAmount"), maxRent));
            }
            return p;
        };
    }
}
