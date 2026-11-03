package com.college.placement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.college.placement.entity.Student;
import com.college.placement.enums.OfferStatus;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserEmail(String email);

    @Query("select s from Student s where (:department is null or lower(s.department) = lower(:department)) "
            + "and (:minCgpa is null or s.cgpa >= :minCgpa) "
            + "and (:year is null or s.graduationYear = :year)")
    Page<Student> search(@Param("department") String department, @Param("minCgpa") Double minCgpa,
            @Param("year") Integer year, Pageable pageable);

    @Query("select s from Student s where lower(s.department) = lower(:department) order by s.cgpa desc")
    List<Student> topStudentsOfDepartment(@Param("department") String department);

    @Query("select distinct s from Student s join s.skills sk where lower(sk.name) = lower(:skill)")
    List<Student> findBySkillName(@Param("skill") String skill);

    @Query("select a.student from Application a where a.jobDrive.id = :driveId order by a.student.cgpa desc")
    List<Student> findAppliedToDrive(@Param("driveId") Long driveId);

    @Query("select s.department as department, count(s) as total, avg(s.cgpa) as averageCgpa "
            + "from Student s group by s.department order by s.department")
    List<DepartmentSummary> departmentStatsJpql();

    @Query(value = "SELECT * FROM students WHERE cgpa >= :minCgpa ORDER BY cgpa DESC", nativeQuery = true)
    List<Student> findByMinCgpaNative(@Param("minCgpa") double minCgpa);

    @Query(value = "SELECT s.* FROM students s LEFT JOIN applications a ON a.student_id = s.id "
            + "WHERE a.id IS NULL ORDER BY s.id", nativeQuery = true)
    List<Student> findStudentsWithoutApplicationsNative();

    @Query(value = "SELECT s.department AS department, COUNT(s.id) AS total, ROUND(AVG(s.cgpa), 2) AS averageCgpa "
            + "FROM students s GROUP BY s.department ORDER BY s.department", nativeQuery = true)
    List<DepartmentSummary> departmentSummaryNative();

    List<Student> findPlacedNamed(@Param("status") OfferStatus status);

    List<Student> findByMinCgpaNamed(@Param("minCgpa") Double minCgpa);

    interface DepartmentSummary {
        String getDepartment();

        Long getTotal();

        Double getAverageCgpa();
    }
}
