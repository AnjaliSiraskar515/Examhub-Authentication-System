package com.example.examauth.student_profile.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FaceVerificationService {

    private static final String AI_FACE_VERIFY_URL = "http://localhost:5001/face_verify";

    private final RestTemplate restTemplate;

    public FaceVerificationService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Sends stored and live Base64 images to the Flask AI service for face
     * verification.
     *
     * @param storedImage Base64-encoded stored ID card image
     * @param liveImage   Base64-encoded live/webcam image
     * @return Map containing the AI service response (e.g., verified, confidence,
     *         message)
     */
    public Map<String, Object> verifyFace(String storedImage, String liveImage) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("storedImage", storedImage);
            requestBody.put("liveImage", liveImage);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    AI_FACE_VERIFY_URL,
                    requestBody,
                    Map.class);

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.getBody();
                return result;
            }

            Map<String, Object> emptyResponse = new HashMap<>();
            emptyResponse.put("error", "Empty response from AI service");
            emptyResponse.put("verified", false);
            emptyResponse.put("confidence", 0.0);
            return emptyResponse;

        } catch (RestClientException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "AI service unavailable: " + e.getMessage());
            errorResponse.put("verified", false);
            errorResponse.put("confidence", 0.0);
            return errorResponse;
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Unexpected error during face verification: " + e.getMessage());
            errorResponse.put("verified", false);
            errorResponse.put("confidence", 0.0);
            return errorResponse;
        }
    }
}
