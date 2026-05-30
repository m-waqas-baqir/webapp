package com.app.backend.dto.media;

import com.app.backend.entity.LinkedEntityType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PropertyImageResponse {
    Long id;
    LinkedEntityType linkedEntityType;
    Long linkedEntityId;
    String fileName;
    String contentType;
    long byteSize;
    Instant createdAt;
    /** Relative API path to stream bytes (requires Authorization header). */
    String downloadUrl;
}
