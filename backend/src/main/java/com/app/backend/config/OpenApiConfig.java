package com.app.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .servers(List.of(new Server().url("/").description("Current host")))
                .info(new Info()
                        .title("Real Investments API")
                        .version("v1")
                        .description("""
                                Production-oriented REST API for **Real Investments** consultancy: JWT auth (Bearer). Standard JSON envelope:
                                `success`, `message`, `data`, `timestamp`, `traceId` (from `X-Trace-Id`), and `errorCode` on failures.

                                **Roles:** `DIRECTOR` (full), `ADMIN` (masked owner/contact fields where documented), `AGENT` (assigned resources only where documented).

                                **Features:** soft-delete on domain aggregates, audit log rows for mutating `/api/**` calls (when enabled), global search `GET /api/v1/search`.

                                Authenticate via `POST /auth/login` or register via `POST /auth/register` (creates `AGENT`).
                                Optional seed users (non-test profile): see logs after startup (`director@seed.local`, password `ChangeMeSeed#2026`).
                                """)
                        .contact(new Contact().name("Real Investments").email("support@realinvestments.local"))
                        .license(new License().name("Proprietary").url("https://realinvestments.local")))
                .externalDocs(new ExternalDocumentation()
                        .description("JWT bearer auth")
                        .url("https://swagger.io/docs/specification/authentication/bearer-authentication/"))
                .tags(List.of(
                        new Tag().name("Authentication").description("Login and registration"),
                        new Tag().name("Account").description("Current user profile"),
                        new Tag().name("Search").description("Cross-domain search"),
                        new Tag().name("Plots").description("Inventory plots with RBAC"),
                        new Tag().name("Rentals").description("Rental properties"),
                        new Tag().name("Owners").description("Owner registry with masked responses"),
                        new Tag().name("Listings").description("Real Investments listings"),
                        new Tag().name("Reports").description("Director-only reports"),
                        new Tag().name("Phases").description("Plot hierarchy"),
                        new Tag().name("Khayabans").description("Streets within a phase")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearerScheme()));
    }

    private static io.swagger.v3.oas.models.security.SecurityScheme bearerScheme() {
        return new io.swagger.v3.oas.models.security.SecurityScheme()
                .name(BEARER_SCHEME)
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste the JWT value from `POST /auth/login` (Authorization: Bearer &lt;token&gt;).");
    }
}
