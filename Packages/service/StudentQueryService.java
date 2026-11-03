package com.college.placement.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.config.AppConfig;
import com.college.placement.dto.StudentDto.DepartmentStatResponse;
import com.college.placement.dto.StudentDto.StudentResponse;
import com.college.placement.entity.Student;
import com.college.placement.enums.OfferStatus;
import com.college.placement.mapper.StudentMapper;
import com.college.placement.repository.StudentCustomRepository;
import com.college.placement.repository.StudentRepository;

@Service
@Transactional(readOnly = true)
public class StudentQueryService {

    private final StudentRepository students;
    private final StudentCustomRepository custom;
    private final StudentMapper mapper;

    public StudentQueryService(StudentRepository students, StudentCustomRepository custom, StudentMapper mapper) {
        this.students = students;
        this.custom = custom;
        this.mapper = mapper;
    }

    // ----- JPQL -----
    public List<StudentResponse> topStudentsOfDepartment(String department) {
        return toResponses(students.topStudentsOfDepartment(department));
    }

    public List<StudentResponse> bySkill(String skill) {
        return toResponses(students.findBySkillName(skill));
    }

    public List<StudentResponse> appliedToDrive(Long driveId) {
        return toResponses(students.findAppliedToDrive(driveId));
    }

    @Cacheable(cacheNames = AppConfig.DEPARTMENT_STATS_CACHE, key = "'all'")
    public List<DepartmentStatResponse> departmentStats() {
        return students.departmentStatsJpql().stream()
                .map(row -> new DepartmentStatResponse(row.getDepartment(), row.getTotal(), row.getAverageCgpa()))
                .toList();
    }

    public List<StudentResponse> byMinCgpaNative(double minCgpa) {
        return toResponses(students.findByMinCgpaNative(minCgpa));
    }

    public List<StudentResponse> withoutApplications() {
        return toResponses(students.findStudentsWithoutApplicationsNative());
    }

    public List<DepartmentStatResponse> departmentSummaryNative() {
        return students.departmentSummaryNative().stream()
                .map(row -> new DepartmentStatResponse(row.getDepartment(), row.getTotal(), row.getAverageCgpa()))
                .toList();
    }

    public List<StudentResponse> placedStudents() {
        return toResponses(students.findPlacedNamed(OfferStatus.ACCEPTED));
    }

    public List<StudentResponse> byMinCgpaNamed(Double minCgpa) {
        return toResponses(students.findByMinCgpaNamed(minCgpa));
    }

    public List<StudentResponse> customSearch(String department, Double minCgpa, Integer graduationYear,
            String skill) {
        return toResponses(custom.searchStudents(department, minCgpa, graduationYear, skill));
    }

    public Double averageCgpa(String department) {
        Double average = custom.findAverageCgpa(department);
        return average == null ? 0.0 : average;
    }

    private List<StudentResponse> toResponses(List<Student> students) {
        return students.stream().map(mapper::toResponse).toList();
    }
}
