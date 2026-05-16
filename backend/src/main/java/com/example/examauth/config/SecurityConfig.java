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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import java.util.List;

@Configuration
@EnableMethodSecurity   // enables @PreAuthorize / @PostAuthorize on methods
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
                        .requestMatchers("/api/student/exams/register").hasAnyRole("STUDENT", "SUPER_ADMIN")
                        .requestMatchers("/api/student/registrations/upload").hasAnyRole("STUDENT", "SUPER_ADMIN")
                        // Institution onboarding is public — no login exists yet at this stage
                        .requestMatchers("/api/institution/register").permitAll()
                        // ── Role-protected API endpoints ───────────────────────────
                        .requestMatchers("/api/profile/**").authenticated()
                        .requestMatchers("/api/student/**").hasAnyRole("STUDENT", "SUPER_ADMIN")
                        .requestMatchers("/api/supervisor/**", "/api/biometric/**").hasAnyRole("SUPERVISOR", "HEAD_SUPERVISOR", "SUPER_ADMIN")
                        .requestMatchers("/api/head-supervisor/**").hasAnyRole("HEAD_SUPERVISOR", "SUPERVISOR", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("UNIVERSITY_ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/institution/**").hasAnyRole("UNIVERSITY_ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Dev only: Allow all origins so it works across different live servers, ports, or "Run" buttons
        configuration.setAllowedOriginPatterns(List.of("*"));
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

