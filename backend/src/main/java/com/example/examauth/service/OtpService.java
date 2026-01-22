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

        // Remove any old OTP for same email
        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);
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
    public boolean sendPhoneOtp(String phone) {
        // Fast2SMS typically expects 10 digit number without +91 for bulk v2,
        // but verify your specific route needs. For now, we strip non-digits.
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        otpRepository.findByPhone(cleanPhone).ifPresent(otpRepository::delete);
        otpRepository.save(new OtpEntity(null, cleanPhone, otp, expiry));

        try {
            if (fast2SmsApiKey == null || fast2SmsApiKey.contains("YOUR_FAST2SMS_API_KEY")) {
                System.out
                        .println("⚠️ Fast2SMS API Key is not set. SMS to " + cleanPhone + " : " + otp + " (SIMULATED)");
                return true;
            }

            // Fast2SMS API Call
            String url = "https://www.fast2sms.com/dev/bulkV2";
            String message = "Your ExamHub OTP is: " + otp;

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", fast2SmsApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Example payload for bulkV2.
            // Note: 'route' usually 'v3' or 'q' for quick transactional.
            // Adjust according to user's plan if needed. 'dlt_te_id' might be needed for
            // Indian routes.
            // Using logic common for generic transactional.
            // But usually 'message', 'language', 'route', 'numbers' are query params or
            // body.
            // Let's use the simplest query param method for now if possible, or body.
            // Bulk V2 usually accepts JSON body.

            String requestJson = String.format(
                    "{\"route\" : \"q\", \"message\" : \"%s\", \"language\" : \"english\", \"flash\" : 0, \"numbers\" : \"%s\"}",
                    message, cleanPhone);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            System.out.println("📱 Fast2SMS Response: " + response.getBody());

            return response.getStatusCode().is2xxSuccessful();

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
        Optional<OtpEntity> entity = otpRepository.findByEmail(email);
        if (entity.isEmpty())
            return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid)
            otpRepository.delete(data);
        return valid;
    }

    // =====================================
    // VERIFY PHONE OTP
    // =====================================
    public boolean verifyPhoneOtp(String phone, String otp) {
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.length() > 10) {
            cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
        }

        Optional<OtpEntity> entity = otpRepository.findByPhone(cleanPhone);
        if (entity.isEmpty())
            return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp) && data.getExpiryTime().isAfter(LocalDateTime.now());
        if (valid)
            otpRepository.delete(data);
        return valid;
    }
}
