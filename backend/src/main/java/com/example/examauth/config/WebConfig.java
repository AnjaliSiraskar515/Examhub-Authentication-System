package com.example.examauth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve profile/document uploads
        java.io.File uploadsDir = new java.io.File(uploadDir);
        if (!uploadsDir.isAbsolute()) {
            uploadsDir = new java.io.File(System.getProperty("user.dir"), uploadDir);
        }
        String uploadsAbsolute = uploadsDir.getAbsolutePath();
        if (!uploadsAbsolute.endsWith("/") && !uploadsAbsolute.endsWith("\\")) {
            uploadsAbsolute = uploadsAbsolute + java.io.File.separator;
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsAbsolute);

        // Serve university logo uploads (stored in uploads/logo/ under working dir)
        String logoDir = new java.io.File(System.getProperty("user.dir"), "uploads/logo").getAbsolutePath();
        registry.addResourceHandler("/uploads/logo/**")
                .addResourceLocations("file:" + logoDir + "/");

        // NOTE: Frontend HTML/CSS/JS files are served automatically by Spring Boot
        // from src/main/resources/static/ — no custom handler needed here.
    }
}

