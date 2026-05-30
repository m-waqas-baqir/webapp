package com.app.backend.service.impl;

import com.app.backend.dto.search.GlobalSearchResponse;
import com.app.backend.dto.search.SearchHit;
import com.app.backend.entity.Khayaban;
import com.app.backend.entity.Listing;
import com.app.backend.entity.Owner;
import com.app.backend.entity.Phase;
import com.app.backend.entity.Plot;
import com.app.backend.entity.RentalProperty;
import com.app.backend.entity.UserRole;
import com.app.backend.repository.KhayabanRepository;
import com.app.backend.repository.ListingRepository;
import com.app.backend.repository.OwnerRepository;
import com.app.backend.repository.PhaseRepository;
import com.app.backend.repository.PlotRepository;
import com.app.backend.repository.RentalPropertyRepository;
import com.app.backend.repository.SoftDeleteSpecifications;
import com.app.backend.security.UserPrincipal;
import com.app.backend.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GlobalSearchServiceImpl implements GlobalSearchService {

    private final OwnerRepository ownerRepository;
    private final ListingRepository listingRepository;
    private final PlotRepository plotRepository;
    private final RentalPropertyRepository rentalPropertyRepository;
    private final PhaseRepository phaseRepository;
    private final KhayabanRepository khayabanRepository;

    @Override
    @Transactional(readOnly = true)
    public GlobalSearchResponse search(UserPrincipal viewer, String query, int limitPerType) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters");
        }
        int cap = Math.min(Math.max(limitPerType, 1), 25);
        var pageable = PageRequest.of(0, cap, Sort.by("id").ascending());
        String likePattern = likeContains(q);

        List<SearchHit> hits = new ArrayList<>();

        if (viewer.getRole() != UserRole.AGENT) {
            Specification<Owner> ownerSpec = Specification.where(SoftDeleteSpecifications.<Owner>notDeleted())
                    .and((root, cq, cb) -> cb.like(cb.lower(root.get("name")), likePattern, '\\'));
            ownerRepository.findAll(ownerSpec, pageable).getContent().forEach(o ->
                    hits.add(new SearchHit("OWNER", o.getId(), o.getName(), "Owner registry"))
            );
        }
        Specification<Listing> listingSpec = Specification.where(SoftDeleteSpecifications.<Listing>notDeleted())
                .and((root, cq, cb) -> cb.like(cb.lower(root.get("title")), likePattern, '\\'));
        listingRepository.findAll(listingSpec, pageable).getContent().forEach(l ->
                hits.add(new SearchHit("LISTING", l.getId(), l.getTitle(), "Listing"))
        );
        Specification<Plot> plotSpec = Specification.where(SoftDeleteSpecifications.<Plot>notDeleted())
                .and((root, cq, cb) -> cb.like(cb.lower(root.get("plotNumber")), likePattern, '\\'));
        plotRepository.findAll(plotSpec, pageable).getContent().forEach(p ->
                hits.add(new SearchHit("PLOT", p.getId(), p.getPlotNumber(), "Plot inventory"))
        );
        Specification<RentalProperty> rentalSpec = Specification.where(SoftDeleteSpecifications.<RentalProperty>notDeleted())
                .and((root, cq, cb) -> cb.or(
                        cb.like(cb.lower(root.get("title")), likePattern, '\\'),
                        cb.like(cb.lower(root.get("address")), likePattern, '\\')));
        rentalPropertyRepository.findAll(rentalSpec, pageable).getContent().forEach(r ->
                hits.add(new SearchHit("RENTAL_PROPERTY", r.getId(), r.getTitle(), r.getAddress()))
        );
        Specification<Phase> phaseSpec = Specification.where(SoftDeleteSpecifications.<Phase>notDeleted())
                .and((root, cq, cb) -> cb.like(cb.lower(root.get("name")), likePattern, '\\'));
        phaseRepository.findAll(phaseSpec, pageable).getContent().forEach(p ->
                hits.add(new SearchHit("PHASE", p.getId(), p.getName(), "Plot phase"))
        );
        Specification<Khayaban> khSpec = Specification.where(SoftDeleteSpecifications.<Khayaban>notDeleted())
                .and((root, cq, cb) -> cb.like(cb.lower(root.get("name")), likePattern, '\\'));
        khayabanRepository.findAll(khSpec, pageable).getContent().forEach(k ->
                hits.add(new SearchHit("KHAYABAN", k.getId(), k.getName(), "Street / block"))
        );

        return new GlobalSearchResponse(q, cap, hits, hits.size());
    }

    private static String likeContains(String raw) {
        String lowered = raw.toLowerCase(Locale.ROOT);
        String escaped = lowered.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
