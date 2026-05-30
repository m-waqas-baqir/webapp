package com.app.backend.dto.rentals;

import com.app.backend.entity.RentalPropertyStatus;
import com.app.backend.entity.RentalPropertyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RentalPropertyRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private RentalPropertyType type;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal rentAmount;

    @NotNull
    private RentalPropertyStatus status;

    /** Required for DIRECTOR/ADMIN creates; agents omit (draft without registry owner). */
    private Long ownerId;

    /** Must reference an existing user with role AGENT when set. */
    private Long assignedAgentId;
}
