package com.example.examauth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BrevoEmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final String senderEmail = "siraskar2005@gmail.com";
    private final String senderName = "ExamHub";

    public void sendEmail(String toEmail, String subject, String content, boolean isHtml) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String url = "https://api.brevo.com/v3/smtp/email";
                
                String contentKey = isHtml ? "htmlContent" : "textContent";
                
                // Escape quotes and newlines for JSON
                String escapedContent = content.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");

                String payload = String.format(
                    "{" +
                    "\"sender\":{\"name\":\"%s\",\"email\":\"%s\"}," +
                    "\"to\":[{\"email\":\"%s\"}]," +
                    "\"subject\":\"%s\"," +
                    "\"%s\":\"%s\"" +
                    "}", senderName, senderEmail, toEmail, subject, contentKey, escapedContent
                );

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("api-key", brevoApiKey);
                headers.set("accept", "application/json");

                HttpEntity<String> request = new HttpEntity<>(payload, headers);
                restTemplate.postForEntity(url, request, String.class);
                
                System.out.println("📧 Email dispatched to: " + toEmail + " via Brevo");
            } catch (Exception e) {
                System.err.println("❌ Failed to dispatch email to: " + toEmail + " due to Brevo API error: " + e.getMessage());
            }
        });
    }
}
