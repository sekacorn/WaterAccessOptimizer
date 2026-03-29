package com.water.auth.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequiredSecretsValidator {

    @Value("${SPRING_DATASOURCE_URL:}")
    private String datasourceUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:}")
    private String datasourceUsername;

    @Value("${SPRING_DATASOURCE_PASSWORD:}")
    private String datasourcePassword;

    @Value("${JWT_SECRET:}")
    private String jwtSecret;

    @PostConstruct
    void validate() {
        require("SPRING_DATASOURCE_URL", datasourceUrl);
        require("SPRING_DATASOURCE_USERNAME", datasourceUsername);
        require("SPRING_DATASOURCE_PASSWORD", datasourcePassword);
        require("JWT_SECRET", jwtSecret);
    }

    private void require(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
    }
}
