package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.KhayabanRequest;
import com.app.backend.dto.plots.KhayabanResponse;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface KhayabanService {

    PageResponse<KhayabanResponse> list(Optional<Long> phaseId, Pageable pageable);

    KhayabanResponse get(Long id);

    KhayabanResponse create(UserPrincipal actor, KhayabanRequest request);

    KhayabanResponse update(UserPrincipal actor, Long id, KhayabanRequest request);

    void delete(UserPrincipal actor, Long id);
}
