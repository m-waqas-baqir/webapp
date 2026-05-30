package com.app.backend.dto.listing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotBlank
    @Size(max = 200)
    private String ownerName;

    @NotBlank
    @Email
    @Size(max = 200)
    private String ownerEmail;

    /** Must reference an existing user with role AGENT when set. */
    private Long assignedAgentId;
}
