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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Autowired
    private JavaMailSender mailSender; // ✅ Injects Gmail SMTP sender

    @Value("${spring.mail.username}")
    private String senderEmail;

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

        try {
            // ✅ Compose and send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom(senderEmail); // 👈 Dynamically set from properties
            message.setSubject("ExamHub OTP Verification");
            message.setText(
                    "Hello,\n\nYour ExamHub verification OTP is: " + otp +
                            "\n\nThis code is valid for 5 minutes.\n" +
                            "Do not share it with anyone.\n\n" +
                            "- ExamHub Authentication System");

            // ✅ Send email
            mailSender.send(message);

            System.out.println("📧 OTP email sent to: " + email);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send OTP email to: " + email);
            return false;
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
