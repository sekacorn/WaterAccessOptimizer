package com.water.gateway.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RequiredSecretsValidator {

    @Value("${JWT_SECRET:}")
    private String jwtSecret;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("Missing required environment variable: JWT_SECRET");
        }
    }
}
