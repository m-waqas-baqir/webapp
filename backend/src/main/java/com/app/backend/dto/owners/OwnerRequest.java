package com.app.backend.dto.owners;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OwnerRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String contactInfo;

    @NotBlank
    @Size(min = 5, max = 20)
    private String cnic;
}
