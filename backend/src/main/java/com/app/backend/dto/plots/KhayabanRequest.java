package com.app.backend.dto.plots;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KhayabanRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Long phaseId;
}
