package com.app.backend.dto.plots;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KhayabanResponse {

    Long id;
    String name;
    Long phaseId;
}
