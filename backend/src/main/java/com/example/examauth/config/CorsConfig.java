package com.example.examauth.config;

/**
 * CORS is configured centrally in {@link SecurityConfig#corsConfigurationSource()}.
 *
 * Allowed origins:
 *   - http://localhost:8080  (same-origin when frontend is served by Spring Boot)
 *   - http://127.0.0.1:8080 (same-origin alias)
 *   - http://localhost:5500  (VS Code Live Server — local development only)
 *   - http://127.0.0.1:5500 (VS Code Live Server alias)
 *
 * In production, only the 8080 origins apply because frontend files are bundled
 * inside the JAR under src/main/resources/static/.
 */
public class CorsConfig {
    // No beans here — see SecurityConfig.corsConfigurationSource()
}

