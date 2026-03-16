package com.example.examauth.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class AiVerificationClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String AI_VERIFY_URL = "http://localhost:5001/verify_document";

    public Map<String, Object> verifyDocument(MultipartFile document) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            Map<String, Object> body = new HashMap<>();
            body.put("document", document.getResource());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    AI_VERIFY_URL,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);
            return response.getBody();

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("confidence", 0.0);
            error.put("message", "AI service not reachable");
            return error;
        }
    }
}