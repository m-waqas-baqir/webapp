package com.app.backend.service;

import com.app.backend.dto.PageResponse;
import com.app.backend.dto.rentals.RentalPropertyRequest;
import com.app.backend.dto.rentals.RentalPropertyResponse;
import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.app.backend.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

public interface RentalPropertyService {

    PageResponse<RentalPropertyResponse> list(
            UserPrincipal viewer,
            Optional<RentalPropertyType> type,
            Optional<RentalPropertyStatus> status,
            Optional<BigDecimal> minRent,
            Optional<BigDecimal> maxRent,
            Pageable pageable
    );

    RentalPropertyResponse get(UserPrincipal viewer, Long id);

    RentalPropertyResponse create(UserPrincipal actor, RentalPropertyRequest request);

    RentalPropertyResponse update(UserPrincipal actor, Long id, RentalPropertyRequest request);

    void delete(UserPrincipal actor, Long id);
}
