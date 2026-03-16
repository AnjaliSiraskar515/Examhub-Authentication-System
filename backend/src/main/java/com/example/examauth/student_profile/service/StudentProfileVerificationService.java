package com.example.examauth.student_profile.service;

import com.example.examauth.student_profile.dto.FaceVerificationRequestDTO;
import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudentProfileVerificationService {

    private final StudentProfileRepository studentProfileRepository;
    private final FaceVerificationService faceVerificationService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public StudentProfileVerificationService(
            StudentProfileRepository studentProfileRepository,
            FaceVerificationService faceVerificationService) {
        this.studentProfileRepository = studentProfileRepository;
        this.faceVerificationService = faceVerificationService;
    }

    /**
     * Verifies a student's face against their ID card image via the AI service.
     * On successful verification, saves the ID card image to disk and locks the
     * profile.
     *
     * @param request DTO containing profileId, idCardImage (Base64), and liveImage
     *                (Base64)
     * @return Map with verification result (verified, confidence, message) or AI
     *         error response
     * @throws RuntimeException if the profile is not found
     */
    public Map<String, Object> verifyAndLockProfile(FaceVerificationRequestDTO request) {
        // 1. Fetch profile — throw if not found
        StudentProfile profile = studentProfileRepository.findById(request.getProfileId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // 2. Call AI face verification
        Map<String, Object> aiResponse = faceVerificationService.verifyFace(
                request.getIdCardImage(),
                request.getLiveImage());

        // 3. Check AI result
        Object verifiedObj = aiResponse.get("verified");
        boolean isVerified = Boolean.TRUE.equals(verifiedObj);
        double confidence = extractConfidence(aiResponse);

        // Extra safety: low confidence fails even if AI says verified
        if (confidence < 75.0) {
            Map<String, Object> failResponse = new HashMap<>();
            failResponse.put("verified", false);
            failResponse.put("confidence", confidence);
            failResponse.put("message", "Face verification confidence too low");
            return failResponse;
        }

        if (isVerified) {
            // 4. Decode Base64 image and save to uploads/profile/profile_{id}.jpg
            String savedFilePath = saveIdCardImage(request.getProfileId(), request.getIdCardImage());

            // 5. Update and lock profile
            profile.setVerified(true);
            profile.setProfileLocked(true);
            profile.setPhotoPath(savedFilePath);
            studentProfileRepository.save(profile);

            // 6. Return success map
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("verified", true);
            successResponse.put("confidence", confidence);
            successResponse.put("message", "Profile verified and locked");
            return successResponse;
        }

        // 7. Verification failed — return AI response as-is
        return aiResponse;
    }

    private double extractConfidence(Map<String, Object> aiResponse) {
        Object confidenceObj = aiResponse.get("confidence");
        if (confidenceObj instanceof Number) {
            return ((Number) confidenceObj).doubleValue();
        }
        try {
            return confidenceObj != null ? Double.parseDouble(confidenceObj.toString()) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Decodes a Base64 image string and writes it to disk.
     *
     * @param profileId   the profile ID (used for file naming)
     * @param base64Image the Base64-encoded image string (may include data URL
     *                    prefix)
     * @return the relative saved file path: uploads/profile/profile_{id}.jpg
     */
    private String saveIdCardImage(Long profileId, String base64Image) {
        try {
            // Strip data URL prefix if present (e.g. "data:image/jpeg;base64,...")
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                base64Data = base64Image.split(",")[1];
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Resolve upload directory (handle both absolute and relative paths)
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.isAbsolute()) {
                uploadDirFile = new File(System.getProperty("user.dir"), uploadDir);
            }

            // Ensure uploads/profile/ sub-directory exists
            Path profileDir = Paths.get(uploadDirFile.getAbsolutePath(), "profile");
            Files.createDirectories(profileDir);

            // Write image file
            String fileName = "profile_" + profileId + ".jpg";
            Path filePath = profileDir.resolve(fileName);
            Files.write(filePath, imageBytes);

            // Return relative path for storage in DB
            return "uploads/profile/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save ID card image for profile " + profileId + ": " + e.getMessage(),
                    e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid Base64 image data for profile " + profileId + ": " + e.getMessage(), e);
        }
    }
}
