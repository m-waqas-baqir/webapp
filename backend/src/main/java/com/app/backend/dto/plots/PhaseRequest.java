package com.app.backend.dto.plots;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhaseRequest {

    @NotBlank
    @Size(max = 200)
    private String name;
}
