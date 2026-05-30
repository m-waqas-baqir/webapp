package com.app.backend.dto.plots;

import com.app.backend.entity.PlotStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlotResponse {

    Long id;
    String plotNumber;
    BigDecimal size;
    BigDecimal price;
    PlotStatus status;
    Long phaseId;
    Long khayabanId;

    /** Registry {@link com.app.backend.entity.Owner} id — omitted for AGENT responses. */
    Long ownerId;

    /** Display label for staff tables/dropdowns — omitted for AGENT. */
    String ownerName;

    Long assignedAgentId;

    /** Assigned agent display name — omitted for AGENT. */
    String assignedAgentName;

    /** Whether the current viewer may PUT this plot (AGENT: only if creator). */
    Boolean canMutate;
}
