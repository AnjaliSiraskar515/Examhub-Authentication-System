package com.example.examauth.service;

import com.example.examauth.model.OtpEntity;
import com.example.examauth.repo.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
// Removed Spring Mail imports as we now use Brevo HTTP API
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    private final String senderEmail = "siraskar2005@gmail.com";

    @Value("${fast2sms.api.key}")
    private String fast2SmsApiKey;

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    // =====================================
    // SEND EMAIL OTP
    // =====================================
    public boolean sendEmailOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Remove all old OTPs for same email
        otpRepository.deleteByEmail(email);
        otpRepository.save(new OtpEntity(email, null, otp, expiry));

        // ✅ ALWAYS Log OTP for troubleshooting/demo purposes
        System.out.println("🔐 GENERATED EMAIL OTP for " + email + ": " + otp);

        try {
            // ✅ Send email asynchronously via Brevo HTTP API so it doesn't block the UI
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    String url = "https://api.brevo.com/v3/smtp/email";
                    
                    String payload = String.format(
                        "{" +
                        "\"sender\":{\"name\":\"ExamHub\",\"email\":\"%s\"}," +
                        "\"to\":[{\"email\":\"%s\"}]," +
                        "\"subject\":\"ExamHub OTP Verification\"," +
                        "\"htmlContent\":\"<html><body><h3>Hello,</h3><p>Your ExamHub verification OTP is: <strong>%s</strong></p><p>This code is valid for 5 minutes.</p><p>Do not share it with anyone.</p><p>- ExamHub Authentication System</p></body></html>\"" +
                        "}", senderEmail, email, otp
                    );

                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("api-key", brevoApiKey);
                    headers.set("accept", "application/json");

                    HttpEntity<String> request = new HttpEntity<>(payload, headers);
                    restTemplate.postForEntity(url, request, String.class);
                    
                    System.out.println("📧 OTP email dispatched to: " + email + " via Brevo");
                } catch (Exception e) {
                    System.err.println("❌ Failed to dispatch OTP email to: " + email + " due to Brevo API error: " + e.getMessage());
                }
            });

            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to compose OTP email: " + e.getMessage());
            return true; // Return true to allow flow to proceed in development
        }
    }

    // =====================================
    // SEND PHONE OTP (Fast2SMS)
    // =====================================
    // =====================================
    // SEND PHONE OTP (Fast2SMS)
    // =====================================
    public boolean sendPhoneOtp(String phone) {
        // Fast2SMS typically expects 10 digit number without +91 for bulk v2,
        // but verify your specific route needs. For now, we strip non-digits.
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Remove old OTPs (handle duplicates)
        otpRepository.findByPhone(cleanPhone).forEach(otpRepository::delete);
        otpRepository.save(new OtpEntity(null, cleanPhone, otp, expiry));

        // ✅ ALWAYS Log OTP for troubleshooting/demo purposes
        System.out.println("🔐 GENERATED MOBILE OTP for " + cleanPhone + ": " + otp);

        try {
            if (fast2SmsApiKey == null || fast2SmsApiKey.isBlank() || fast2SmsApiKey.contains("YOUR_FAST2SMS_API_KEY")) {
                System.out.println("⚠️ Fast2SMS API Key is not set. SMS to " + cleanPhone + " : " + otp + " (SIMULATED)");
                return true;
            }

            // Fast2SMS API Call — using 'q' (Quick) route
            String url = "https://www.fast2sms.com/dev/bulkV2";
            String smsMessage = "Your ExamHub OTP is: " + otp;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", fast2SmsApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            headers.set("Accept", "*/*");

            // 'q' route
            String requestJson = String.format(
                    "{\"route\":\"q\",\"message\":\"%s\",\"language\":\"english\",\"flash\":0,\"numbers\":\"%s\"}",
                    smsMessage, cleanPhone);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            System.out.println("📱 Fast2SMS Response: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("❌ Fast2SMS API Error: " + e.getResponseBodyAsString());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send SMS to: " + cleanPhone);
            return false;
        }
    }

    // =====================================
    // VERIFY EMAIL OTP
    // =====================================
    public boolean verifyEmailOtp(String email, String otp) {
        java.util.List<OtpEntity> entities = otpRepository.findByEmail(email);
        if (entities.isEmpty())
            return false;

        // Find any valid OTP match
        Optional<OtpEntity> validOtp = entities.stream()
                .filter(e -> e.getOtp().equals(otp) && e.getExpiryTime().isAfter(LocalDateTime.now()))
                .findFirst();

        if (validOtp.isPresent()) {
            OtpEntity entity = validOtp.get();
            entity.setVerified(true);
            otpRepository.save(entity);
            return true;
        }
        return false;
    }

    public boolean isEmailVerified(String email) {
        java.util.List<OtpEntity> entities = otpRepository.findByEmail(email);
        return entities.stream().anyMatch(e -> e.isVerified() && e.getExpiryTime().isAfter(LocalDateTime.now()));
    }

    public void clearVerifiedEmailOtp(String email) {
        otpRepository.deleteByEmail(email);
    }

    // =====================================
    // VERIFY PHONE OTP
    // =====================================
    public boolean verifyPhoneOtp(String phone, String otp) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        java.util.List<OtpEntity> entities = otpRepository.findByPhone(cleanPhone);
        if (entities.isEmpty())
            return false;

        // Find any valid OTP match
        Optional<OtpEntity> validOtp = entities.stream()
                .filter(e -> e.getOtp().equals(otp) && e.getExpiryTime().isAfter(LocalDateTime.now()))
                .findFirst();

        if (validOtp.isPresent()) {
            OtpEntity entity = validOtp.get();
            entity.setVerified(true);
            otpRepository.save(entity);
            return true;
        }
        return false;
    }

    public boolean isPhoneVerified(String phone) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }
        java.util.List<OtpEntity> entities = otpRepository.findByPhone(cleanPhone);
        return entities.stream().anyMatch(e -> e.isVerified() && e.getExpiryTime().isAfter(LocalDateTime.now()));
    }

    public void clearVerifiedPhoneOtp(String phone) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }
        otpRepository.deleteByPhone(cleanPhone);
    }
}
