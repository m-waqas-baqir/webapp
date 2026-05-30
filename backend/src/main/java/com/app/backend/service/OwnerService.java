package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.owners.OwnerRequest;
import com.app.backend.dto.owners.OwnerResponse;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

public interface OwnerService {

    PageResponse<OwnerResponse> list(UserPrincipal viewer, Pageable pageable);

    OwnerResponse get(UserPrincipal viewer, Long id);

    OwnerResponse create(UserPrincipal actor, OwnerRequest request);

    OwnerResponse update(UserPrincipal actor, Long id, OwnerRequest request);

    void delete(UserPrincipal actor, Long id);
}
