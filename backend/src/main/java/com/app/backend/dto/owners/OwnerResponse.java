package com.app.backend.dto.owners;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Role-dependent projection: DIRECTOR sees raw PII; ADMIN sees masked fields; AGENT minimal disclosure.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OwnerResponse {

    Long id;

    /** DIRECTOR: full; ADMIN: masked; AGENT: highly redacted. */
    String name;

    /** DIRECTOR: full; ADMIN: masked; AGENT: omitted. */
    String contactInfo;

    /** DIRECTOR: full; ADMIN: partially masked; AGENT: omitted. */
    String cnic;
}
