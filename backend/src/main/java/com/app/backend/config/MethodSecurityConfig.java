package com.app.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@link org.springframework.security.access.prepost.PreAuthorize} and related annotations.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
