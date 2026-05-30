package com.app.backend.dto.plots;

import com.app.backend.entity.PlotStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlotRequest {

    @NotBlank
    @Size(max = 64)
    private String plotNumber;

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal size;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal price;

    @NotNull
    private PlotStatus status;

    @NotNull
    private Long phaseId;

    @NotNull
    private Long khayabanId;

    private Long ownerId;

    private Long assignedAgentId;
}
