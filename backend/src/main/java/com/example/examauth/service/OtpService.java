package com.example.examauth.service;

import com.example.examauth.model.OtpEntity;
import com.example.examauth.repo.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // ✅ Fast2SMS Config
    @Value("${fast2sms.api.key}")
    private String fast2smsApiKey;

    @Value("${fast2sms.sender}")
    private String fast2smsSender;

    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    // ==================================================
    // SEND EMAIL OTP (UNCHANGED – WORKING)
    // ==================================================
    public boolean sendEmailOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        otpRepository.findByEmail(email).ifPresent(otpRepository::delete);
        otpRepository.save(new OtpEntity(email, null, otp, expiry));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setFrom(senderEmail);
            message.setSubject("ExamHub OTP Verification");
            message.setText(
                    "Hello,\n\nYour ExamHub verification OTP is: " + otp +
                            "\n\nThis code is valid for 5 minutes.\n" +
                            "Do not share it with anyone.\n\n" +
                            "- ExamHub Authentication System");

            mailSender.send(message);

            System.out.println("📧 OTP email sent to: " + email);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send OTP email to: " + email);
            return false;
        }
    }

    // ==================================================
    // SEND PHONE OTP (FAST2SMS)
    // ==================================================
    public boolean sendPhoneOtp(String phone) {
        String otp = generateOtp();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        try {
            // 1. Send SMS via Fast2SMS
            String url = "https://www.fast2sms.com/dev/bulkV2" +
                    "?authorization=" + fast2smsApiKey +
                    "&sender_id=" + fast2smsSender +
                    "&message=Your ExamHub OTP is " + otp +
                    "&language=english" +
                    "&route=q" +
                    "&numbers=" + phone;

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForObject(url, String.class);
            System.out.println("📱 SMS OTP sent to " + phone);

            // 2. Clear old OTPs and Save NEW OTP only if SMS was successful
            List<OtpEntity> existing = otpRepository.findByPhone(phone);
            otpRepository.deleteAll(existing);
            otpRepository.save(new OtpEntity(null, phone, otp, expiry));

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Failed to send SMS to: " + phone);
            throw new RuntimeException("SMS failed");
        }
    }

    // ==================================================
    // VERIFY EMAIL OTP (UNCHANGED)
    // ==================================================
    public boolean verifyEmailOtp(String email, String otp) {
        Optional<OtpEntity> entity = otpRepository.findByEmail(email);
        if (entity.isEmpty())
            return false;

        OtpEntity data = entity.get();
        boolean valid = data.getOtp().equals(otp)
                && data.getExpiryTime().isAfter(LocalDateTime.now());

        if (valid)
            otpRepository.delete(data);

        return valid;
    }

    // ==================================================
    // VERIFY PHONE OTP (FIXED FOR LIST)
    // ==================================================
    public boolean verifyPhoneOtp(String phone, String otp) {
        // ✅ FIXED: findByPhone returns List
        List<OtpEntity> entities = otpRepository.findByPhone(phone);
        if (entities.isEmpty())
            return false;

        // Check if ANY of the OTPs match
        for (OtpEntity data : entities) {
            boolean valid = data.getOtp().equals(otp)
                    && data.getExpiryTime().isAfter(LocalDateTime.now());
            if (valid) {
                otpRepository.delete(data);
                return true;
            }
        }
        return false;
    }
}
