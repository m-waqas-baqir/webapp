package com.app.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HS256 signing settings for issued JWTs.
 */
@Data
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    /**
     * Symmetric key; must be at least 256 bits (32 ASCII chars) for HS256.
     */
    private String secret = "";

    /**
     * Access token lifetime in seconds.
     */
    private long expirationSeconds = 86400;

    private String issuer = "Real Investments";

    private String audience = "";
}
