package com.example.examauth.student_profile.service;

import com.example.examauth.student_profile.model.StudentProfile;
import com.example.examauth.student_profile.repo.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    /**
     * Retrieves a student profile by its ID.
     *
     * @param id the profile ID
     * @return an Optional containing the profile if found
     */
    public Optional<StudentProfile> getProfileById(Long id) {
        return studentProfileRepository.findById(id);
    }

    /**
     * Updates the year field of a student profile.
     * Throws IllegalStateException if the profile is locked.
     *
     * @param id      the profile ID
     * @param newYear the new year value
     * @return the updated StudentProfile
     * @throws IllegalArgumentException if the profile is not found
     * @throws IllegalStateException    if the profile is locked
     */
    public StudentProfile updateYear(Long id, String newYear) {
        StudentProfile profile = studentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found with id: " + id));

        if (profile.isProfileLocked()) {
            throw new IllegalStateException("Profile is locked. Year cannot be updated.");
        }

        profile.setYear(newYear);
        return studentProfileRepository.save(profile);
    }
}
