package com.app.backend.repository;

import com.app.backend.entity.Listing;
import com.app.backend.entity.UserRole;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.jpa.domain.Specification;

/**
 * Data-level filters (agent scope). DIRECTOR/ADMIN see all listings; AGENT only assigned rows.
 */
public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> visibleTo(UserPrincipal principal) {
        if (principal.getRole() == UserRole.AGENT) {
            return (root, query, cb) -> cb.equal(root.get("assignedAgentId"), principal.getId());
        }
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Listing> idAndVisibleTo(Long id, UserPrincipal principal) {
        return visibleTo(principal).and((root, query, cb) -> cb.equal(root.get("id"), id));
    }
}
