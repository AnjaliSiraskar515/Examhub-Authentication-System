package com.example.examauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // ── Static frontend assets (served from /static/) ──────────
                        .requestMatchers(
                                "/",
                                "/*.html",
                                "/*.css",
                                "/*.js",
                                "/*.png",
                                "/*.jpg",
                                "/*.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        // ── Public API endpoints ───────────────────────────────────
                        .requestMatchers("/api/otp/**", "/api/auth/**", "/api/test/**", "/api/debug/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/admit-card/**").permitAll()
                        .requestMatchers("/api/university/**", "/api/stats/**").permitAll()
                        .requestMatchers("/api/student/registrations").permitAll()
                        // ── Role-protected API endpoints ───────────────────────────
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "SUPERADMIN")
                        .requestMatchers("/api/supervisor/**", "/api/biometric/**").hasAnyRole("SUPERVISOR", "SUPERADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("UNIVERSITY_ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/institution/**").hasAnyRole("UNIVERSITY_ADMIN", "SUPERADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Dev only: VS Code Live Server (port 5500). Same-origin (8080) needs no CORS.
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

