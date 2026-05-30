package com.app.backend.dto.rentals;

import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RentalPropertyResponse {

    Long id;
    String title;
    RentalPropertyType type;
    String address;
    BigDecimal rentAmount;
    RentalPropertyStatus status;

    /** Registry {@link com.app.backend.entity.Owner} id — omitted for AGENT when present. */
    Long ownerId;

    String ownerName;

    Long assignedAgentId;

    String assignedAgentName;

    Boolean canMutate;
}
