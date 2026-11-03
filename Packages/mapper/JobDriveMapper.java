package com.college.placement.mapper;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.college.placement.dto.DriveDto.JobDrivePatchRequest;
import com.college.placement.dto.DriveDto.JobDriveRequest;
import com.college.placement.dto.DriveDto.JobDriveResponse;
import com.college.placement.entity.Company;
import com.college.placement.entity.JobDrive;
import com.college.placement.entity.Skill;

@Component
public class JobDriveMapper {

    private final SkillMapper skillMapper;

    public JobDriveMapper(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
    }

    public JobDrive toEntity(JobDriveRequest request, Company company, Set<Skill> skills) {
        JobDrive drive = new JobDrive(company, request.title().trim(), request.location().trim(),
                request.minimumCgpa(), request.eligibleDepartment().trim(), request.applicationDeadline(),
                request.driveDate(), request.description());
        drive.setRequiredSkills(skills);
        return drive;
    }

    public void updateEntity(JobDrive drive, JobDriveRequest request, Company company, Set<Skill> skills) {
        drive.setCompany(company);
        drive.setTitle(request.title().trim());
        drive.setLocation(request.location().trim());
        drive.setMinimumCgpa(request.minimumCgpa());
        drive.setEligibleDepartment(request.eligibleDepartment().trim());
        drive.setApplicationDeadline(request.applicationDeadline());
        drive.setDriveDate(request.driveDate());
        drive.setDescription(request.description());
        drive.setRequiredSkills(skills);
    }

    public void patchEntity(JobDrive drive, JobDrivePatchRequest request) {
        if (request.title() != null) {
            drive.setTitle(request.title().trim());
        }
        if (request.location() != null) {
            drive.setLocation(request.location().trim());
        }
        if (request.minimumCgpa() != null) {
            drive.setMinimumCgpa(request.minimumCgpa());
        }
        if (request.eligibleDepartment() != null) {
            drive.setEligibleDepartment(request.eligibleDepartment().trim());
        }
        if (request.applicationDeadline() != null) {
            drive.setApplicationDeadline(request.applicationDeadline());
        }
        if (request.driveDate() != null) {
            drive.setDriveDate(request.driveDate());
        }
        if (request.description() != null) {
            drive.setDescription(request.description());
        }
    }

    public JobDriveResponse toResponse(JobDrive drive) {
        return new JobDriveResponse(drive.getId(), drive.getCompany().getId(), drive.getCompany().getName(),
                drive.getTitle(), drive.getLocation(), drive.getMinimumCgpa(), drive.getEligibleDepartment(),
                drive.getApplicationDeadline(), drive.getDriveDate(), drive.getStatus(), drive.getDescription(),
                skillMapper.toNames(drive.getRequiredSkills()));
    }
}
