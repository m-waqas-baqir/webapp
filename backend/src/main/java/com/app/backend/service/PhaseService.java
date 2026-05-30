package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PhaseRequest;
import com.app.backend.dto.plots.PhaseResponse;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

public interface PhaseService {

    PageResponse<PhaseResponse> list(Pageable pageable);

    PhaseResponse get(Long id);

    PhaseResponse create(UserPrincipal actor, PhaseRequest request);

    PhaseResponse update(UserPrincipal actor, Long id, PhaseRequest request);

    void delete(UserPrincipal actor, Long id);
}
