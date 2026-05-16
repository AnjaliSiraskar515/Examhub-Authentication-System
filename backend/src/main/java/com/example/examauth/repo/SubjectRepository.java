package com.example.examauth.repo;

import com.example.examauth.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByCode(String code);
    java.util.List<Subject> findAllByCode(String code);

    Optional<Subject> findByCodeAndSemesterAndDepartmentEntityIdAndCourse(String code, Integer semester, Long departmentId, String course);

    // Full exact match
    java.util.List<Subject> findByCourseAndDepartmentEntityIdAndSemester(String course, Long departmentId, Integer semester);

    // Two-param combinations
    java.util.List<Subject> findByCourseAndDepartmentEntityId(String course, Long departmentId);
    java.util.List<Subject> findByCourseAndSemester(String course, Integer semester);
    java.util.List<Subject> findByDepartmentEntityIdAndSemester(Long departmentId, Integer semester);

    // Single-param
    java.util.List<Subject> findByCourse(String course);
    java.util.List<Subject> findByDepartmentEntityId(Long departmentId);
    java.util.List<Subject> findBySemester(Integer semester);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Subject s WHERE s.course = :course AND s.semester = :sem AND s.departmentEntity.college.id = :colId")
    java.util.List<Subject> findByCourseSemesterAndCollege(@org.springframework.data.repository.query.Param("course") String course, @org.springframework.data.repository.query.Param("sem") Integer sem, @org.springframework.data.repository.query.Param("colId") Long colId);
}
