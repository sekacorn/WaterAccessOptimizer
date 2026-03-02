package com.water.data.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 configuration for Swagger UI documentation.
 * Accessible at: http://localhost:8087/swagger-ui.html
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "WaterAccessOptimizer Data Service API",
        version = "1.0.0",
        description = """
            REST API for water access data management and risk assessment.

            ## Features
            - CSV data upload with 5-stage validation pipeline
            - Geospatial queries with PostGIS
            - Multi-criteria risk assessment engine
            - PDF and Excel export functionality
            - Storage quota management

            ## Authentication
            All endpoints require JWT Bearer token authentication (except health checks).
            Obtain token from Auth Service: POST /api/v1/auth/login

            ## Rate Limiting
            - 100 requests per minute per user
            - 10 MB max file upload size

            ## Data Types
            - **HYDRO**: Water quality measurements (pH, arsenic, fluoride, nitrate, E. coli)
            - **COMMUNITY**: Population demographics and water access levels
            - **INFRASTRUCTURE**: Water facility locations and operational status
            """,
        contact = @Contact(
            name = "WaterAccessOptimizer Team",
            email = "support@wateroptimizer.org",
            url = "https://wateroptimizer.org"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(
            description = "Local Development",
            url = "http://localhost:8087"
        ),
        @Server(
            description = "Production",
            url = "https://api.wateroptimizer.org"
        )
    }
)
@SecurityScheme(
    name = "bearerAuth",
    description = "JWT Bearer token authentication. Format: Bearer <token>",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Configuration is done via annotations
}
