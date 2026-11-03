package com.college.placement.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.college.placement.dto.DriveDto.DriveReportResponse;
import com.college.placement.dto.DriveDto.EligibilityResponse;
import com.college.placement.dto.DriveDto.JobDrivePatchRequest;
import com.college.placement.dto.DriveDto.JobDriveRequest;
import com.college.placement.dto.DriveDto.JobDriveResponse;
import com.college.placement.entity.Company;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Student;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.DriveStatus;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.mapper.JobDriveMapper;
import com.college.placement.repository.ApplicationRepository;
import com.college.placement.repository.CompanyRepository;
import com.college.placement.repository.JobDriveRepository;
import com.college.placement.repository.StudentRepository;

@Service
public class JobDriveService {

    private static final Logger log = LoggerFactory.getLogger(JobDriveService.class);

    private final JobDriveRepository drives;
    private final CompanyRepository companies;
    private final StudentRepository students;
    private final ApplicationRepository applications;
    private final SkillService skillService;
    private final JobDriveMapper mapper;

    public JobDriveService(JobDriveRepository drives, CompanyRepository companies, StudentRepository students,
            ApplicationRepository applications, SkillService skillService, JobDriveMapper mapper) {
        this.drives = drives;
        this.companies = companies;
        this.students = students;
        this.applications = applications;
        this.skillService = skillService;
        this.mapper = mapper;
    }

    @Transactional
    public JobDriveResponse create(JobDriveRequest request) {
        Company company = findCompany(request.companyId());
        validateDates(request.applicationDeadline(), request.driveDate());
        JobDrive drive = drives.save(mapper.toEntity(request, company, skillService.resolve(request.requiredSkills())));
        log.info("Job drive created: {}", drive.getTitle());
        return mapper.toResponse(drive);
    }

    @Transactional(readOnly = true)
    public Page<JobDriveResponse> list(Pageable pageable) {
        return drives.findByStatus(DriveStatus.PUBLISHED, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<JobDriveResponse> search(String keyword, String department, Pageable pageable) {
        String text = StringUtils.hasText(keyword) ? keyword.trim() : null;
        String dept = StringUtils.hasText(department) ? department.trim() : null;
        return drives.search(DriveStatus.PUBLISHED, text, dept, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public JobDriveResponse get(Long id, boolean manager) {
        JobDrive drive = find(id);
        if (drive.getStatus() != DriveStatus.PUBLISHED && !manager) {
            throw new NotFoundException("Job drive not found");
        }
        return mapper.toResponse(drive);
    }

    @Transactional
    public JobDriveResponse update(Long id, JobDriveRequest request) {
        JobDrive drive = find(id);
        requireDraft(drive);
        Company company = findCompany(request.companyId());
        validateDates(request.applicationDeadline(), request.driveDate());
        mapper.updateEntity(drive, request, company, skillService.resolve(request.requiredSkills()));
        log.info("Job drive {} updated", id);
        return mapper.toResponse(drive);
    }

    @Transactional
    public JobDriveResponse patch(Long id, JobDrivePatchRequest request) {
        JobDrive drive = find(id);
        requireDraft(drive);
        mapper.patchEntity(drive, request);
        if (request.requiredSkills() != null) {
            drive.setRequiredSkills(skillService.resolve(request.requiredSkills()));
        }
        validateDates(drive.getApplicationDeadline(), drive.getDriveDate());
        log.info("Job drive {} patched", id);
        return mapper.toResponse(drive);
    }

    @Transactional
    public JobDriveResponse publish(Long id) {
        JobDrive drive = find(id);
        drive.publish();
        log.info("Job drive {} published", id);
        return mapper.toResponse(drive);
    }

    @Transactional
    public JobDriveResponse cancel(Long id) {
        JobDrive drive = find(id);
        drive.cancel();
        log.info("Job drive {} cancelled", id);
        return mapper.toResponse(drive);
    }

    @Transactional
    public void delete(Long id) {
        JobDrive drive = find(id);
        if (drive.getStatus() != DriveStatus.DRAFT && drive.getStatus() != DriveStatus.CANCELLED) {
            throw new IllegalStateException("Only DRAFT or CANCELLED drives can be deleted");
        }
        if (applications.existsByJobDriveId(id)) {
            throw new ConflictException("Job drive has applications and cannot be deleted");
        }
        drives.delete(drive);
        log.info("Job drive {} deleted", id);
    }

    @Transactional(readOnly = true)
    public EligibilityResponse eligibility(Long id, String email) {
        Student student = students.findByUserEmail(email)
                .orElseThrow(() -> new NotFoundException("Create your student profile first"));
        JobDrive drive = find(id);
        boolean eligible = drive.getStatus() == DriveStatus.PUBLISHED && drive.isEligible(student);
        return new EligibilityResponse(id, eligible);
    }

    @Transactional(readOnly = true)
    public DriveReportResponse report(Long id) {
        JobDrive drive = find(id);
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            counts.put(status, 0L);
        }
        List<Object[]> rows = applications.countByStatus(id);
        long total = 0;
        for (Object[] row : rows) {
            long count = (Long) row[1];
            counts.put((ApplicationStatus) row[0], count);
            total += count;
        }
        return new DriveReportResponse(id, drive.getTitle(), total, counts);
    }

    @Transactional
    public int closeExpiredDrives() {
        return drives.closeExpired(DriveStatus.CLOSED, DriveStatus.PUBLISHED, LocalDate.now());
    }

    private JobDrive find(Long id) {
        return drives.findById(id).orElseThrow(() -> new NotFoundException("Job drive not found"));
    }

    private Company findCompany(Long id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private void requireDraft(JobDrive drive) {
        if (drive.getStatus() != DriveStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT drives can be edited");
        }
    }

    private void validateDates(LocalDate deadline, LocalDate driveDate) {
        if (driveDate.isBefore(deadline)) {
            throw new IllegalStateException("Drive date cannot be before the application deadline");
        }
    }
}
