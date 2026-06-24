package com.evidencepilot.config.docs;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI and Swagger configuration for the Evidence Pilot REST API.
 * Enables global JWT Bearer authentication in the Swagger UI.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Evidence Pilot API",
                version = "1.0",
                description = "API documentation for the Evidence Pilot prototype"
        ),
        security = @SecurityRequirement(name = "Bearer Authentication")
)
@SecurityScheme(
        name = "Bearer Authentication",
        description = "JWT Authorization header using the Bearer scheme. Enter your token in the input field.",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
