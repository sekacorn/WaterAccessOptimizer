package com.water.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WaterAccessOptimizer - Authentication & Authorization Service
 *
 * This microservice handles:
 * - User registration and login
 * - JWT token generation and validation
 * - Role-based access control (USER, MODERATOR, ADMIN, SUPER_ADMIN, ENTERPRISE_ADMIN)
 * - Permission management
 * - Enterprise user management
 * - User invitations
 * - Audit logging
 *
 * @author WaterAccessOptimizer Team
 * @version 1.0.0
 */
@SpringBootApplication
public class AuthServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApp.class, args);
        System.out.println("=================================================");
        System.out.println("  Authentication Service Started Successfully");
        System.out.println("  Port: 8086");
        System.out.println("  Roles: USER, MODERATOR, ADMIN, ENTERPRISE_ADMIN, SUPER_ADMIN");
        System.out.println("=================================================");
    }
}
