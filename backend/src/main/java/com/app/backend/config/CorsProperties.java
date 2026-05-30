package com.app.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Browser origins allowed to call this API (e.g. Angular dev server on port 4200).
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));
}
