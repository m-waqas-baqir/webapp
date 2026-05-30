package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.plots.PlotRequest;
import com.app.backend.dto.plots.PlotResponse;
import com.app.backend.entity.PlotStatus;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface PlotService {

    PageResponse<PlotResponse> list(
            UserPrincipal viewer,
            Optional<Long> phaseId,
            Optional<Long> khayabanId,
            Optional<PlotStatus> status,
            Optional<BigDecimal> minPrice,
            Optional<BigDecimal> maxPrice,
            Optional<BigDecimal> minSize,
            Optional<BigDecimal> maxSize,
            Pageable pageable
    );

    PlotResponse get(UserPrincipal viewer, Long id);

    PlotResponse create(UserPrincipal actor, PlotRequest request);

    PlotResponse update(UserPrincipal actor, Long id, PlotRequest request);

    void delete(UserPrincipal actor, Long id);
}
