package com.college.placement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.dto.ApplicationDto.ApplicationStatusRequest;
import com.college.placement.entity.Application;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Student;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.DriveStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.ApplicationMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.JobDriveRepository;
import com.college.placement.repository.StudentRepository;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applications;
    private final StudentRepository students;
    private final JobDriveRepository drives;
    private final ApplicationMapper mapper;

    public ApplicationService(ApplicationRepository applications, StudentRepository students,
            JobDriveRepository drives, ApplicationMapper mapper) {
        this.applications = applications;
        this.students = students;
        this.drives = drives;
        this.mapper = mapper;
    }

    @Transactional
    public ApplicationResponse apply(Long driveId, String email) {
        Student student = students.findByUserEmail(email)
                .orElseThrow(() -> new NotFoundException("Create your student profile first"));
        JobDrive drive = drives.findById(driveId).orElseThrow(() -> new NotFoundException("Job drive not found"));
        if (drive.getStatus() != DriveStatus.PUBLISHED) {
            throw new IllegalStateException("Drive is not open for applications");
        }
        if (!drive.isEligible(student)) {
            throw new IllegalStateException("Student is not eligible for this drive");
        }
        if (applications.existsByStudentIdAndJobDriveId(student.getId(), drive.getId())) {
            throw new ConflictException("Already applied to this drive");
        }
        Application application = applications.save(new Application(student, drive));
        log.info("{} applied to drive {}", email, driveId);
        return mapper.toResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> mine(String email, Pageable pageable) {
        return applications.findByStudentUserEmail(email, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> list(Long driveId, ApplicationStatus status, Pageable pageable) {
        return applications.filter(driveId, status, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse get(Long id, String email, boolean staff) {
        return mapper.toResponse(findAuthorized(id, email, staff));
    }

    @Transactional
    public ApplicationResponse shortlist(Long id) {
        Application application = find(id);
        application.shortlist();
        log.info("Application {} shortlisted", id);
        return mapper.toResponse(application);
    }

    @Transactional
    public ApplicationResponse updateStatus(Long id, ApplicationStatusRequest request) {
        if (request.status() != ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Only REJECTED can be set here; use shortlist, interview and offer APIs");
        }
        Application application = find(id);
        ApplicationStatus current = application.getStatus();
        if (current == ApplicationStatus.REJECTED || current == ApplicationStatus.WITHDRAWN
                || current == ApplicationStatus.OFFERED) {
            throw new IllegalStateException("Application is already " + current);
        }
        application.setStatus(ApplicationStatus.REJECTED);
        log.info("Application {} rejected", id);
        return mapper.toResponse(application);
    }

    @Transactional
    public ApplicationResponse cancel(Long id, String email) {
        Application application = findAuthorized(id, email, false);
        ApplicationStatus current = application.getStatus();
        if (current == ApplicationStatus.REJECTED || current == ApplicationStatus.WITHDRAWN
                || current == ApplicationStatus.OFFERED) {
            throw new IllegalStateException("Application cannot be cancelled in status " + current);
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        log.info("Application {} withdrawn by {}", id, email);
        return mapper.toResponse(application);
    }

    @Transactional
    public void delete(Long id) {
        Application application = find(id);
        ApplicationStatus current = application.getStatus();
        if (current != ApplicationStatus.WITHDRAWN && current != ApplicationStatus.REJECTED) {
            throw new IllegalStateException("Only WITHDRAWN or REJECTED applications can be deleted");
        }
        if (!application.getInterviews().isEmpty()) {
            throw new ConflictException("Application has interview history and cannot be deleted");
        }
        applications.delete(application);
        log.info("Application {} deleted", id);
    }

    private Application find(Long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private Application findAuthorized(Long id, String email, boolean staff) {
        Application application = find(id);
        if (!staff && !application.getStudent().getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You can access only your own applications");
        }
        return application;
    }
}
