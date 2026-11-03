package com.college.placement.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.college.placement.config.AppConfig;
import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.dto.StudentDto.StudentPatchRequest;
import com.college.placement.dto.StudentDto.StudentRequest;
import com.college.placement.dto.StudentDto.StudentResponse;
import com.college.placement.entity.AppUser;
import com.college.placement.entity.Student;
import com.college.placement.enums.Role;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.ApplicationMapper;
import com.college.placement.mapper.StudentMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.StudentRepository;
import com.college.placement.repository.UserRepository;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository students;
    private final UserRepository users;
    private final ApplicationRepository applications;
    private final SkillService skillService;
    private final StudentMapper mapper;
    private final ApplicationMapper applicationMapper;

    public StudentService(StudentRepository students, UserRepository users, ApplicationRepository applications,
            SkillService skillService, StudentMapper mapper, ApplicationMapper applicationMapper) {
        this.students = students;
        this.users = users;
        this.applications = applications;
        this.skillService = skillService;
        this.mapper = mapper;
        this.applicationMapper = applicationMapper;
    }

    @CacheEvict(cacheNames = AppConfig.DEPARTMENT_STATS_CACHE, allEntries = true)
    @Transactional
    public StudentResponse create(String email, StudentRequest request) {
        AppUser user = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != Role.STUDENT) {
            throw new ConflictException("Only student accounts can create a student profile");
        }
        if (students.findByUserEmail(email).isPresent()) {
            throw new ConflictException("Student profile already exists");
        }
        Student student = students.save(mapper.toEntity(request, user, skillService.resolve(request.skills())));
        log.info("Student profile created for {}", email);
        return mapper.toResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> list(Pageable pageable) {
        return students.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String department, Double minCgpa, Integer graduationYear,
            Pageable pageable) {
        String dept = StringUtils.hasText(department) ? department.trim() : null;
        return students.search(dept, minCgpa, graduationYear, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse get(Long id, String email, boolean staff) {
        return mapper.toResponse(findAuthorized(id, email, staff));
    }

    @CacheEvict(cacheNames = AppConfig.DEPARTMENT_STATS_CACHE, allEntries = true)
    @Transactional
    public StudentResponse update(Long id, StudentRequest request, String email, boolean staff) {
        Student student = findAuthorized(id, email, staff);
        mapper.updateEntity(student, request, skillService.resolve(request.skills()));
        log.info("Student {} updated", id);
        return mapper.toResponse(student);
    }

    @CacheEvict(cacheNames = AppConfig.DEPARTMENT_STATS_CACHE, allEntries = true)
    @Transactional
    public StudentResponse patch(Long id, StudentPatchRequest request, String email, boolean staff) {
        Student student = findAuthorized(id, email, staff);
        mapper.patchEntity(student, request);
        if (request.skills() != null) {
            student.setSkills(skillService.resolve(request.skills()));
        }
        log.info("Student {} patched", id);
        return mapper.toResponse(student);
    }

    @CacheEvict(cacheNames = AppConfig.DEPARTMENT_STATS_CACHE, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Student student = find(id);
        if (!student.getApplications().isEmpty()) {
            throw new ConflictException("Student has applications and cannot be deleted");
        }
        students.delete(student);
        log.info("Student {} deleted", id);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> history(Long id) {
        find(id);
        return applications.findByStudentIdOrderByCreatedAtDesc(id).stream().map(applicationMapper::toResponse)
                .toList();
    }

    private Student find(Long id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Student not found"));
    }

    private Student findAuthorized(Long id, String email, boolean staff) {
        Student student = find(id);
        if (!staff && !student.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can access only your own profile");
        }
        return student;
    }
}
