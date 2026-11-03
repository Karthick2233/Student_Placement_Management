package com.college.placement.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.DriveStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class DriveDto {

    private DriveDto() {
    }

    public record CompanyRequest(
            @NotBlank String name,
            @Pattern(regexp = "^$|https?://.+", message = "website must be a URL") String website,
            String description) {
    }

    public record CompanyPatchRequest(
            @Pattern(regexp = ".*\\S.*", message = "name must not be blank") String name,
            @Pattern(regexp = "^$|https?://.+", message = "website must be a URL") String website,
            String description) {
    }

    public record CompanyResponse(Long id, String name, String website, String description, boolean approved) {
    }

    public record JobDriveRequest(
            @NotNull Long companyId,
            @NotBlank String title,
            @NotBlank String location,
            @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double minimumCgpa,
            @NotBlank String eligibleDepartment,
            @NotNull LocalDate applicationDeadline,
            @NotNull LocalDate driveDate,
            String description,
            Set<String> requiredSkills) {
    }

    public record JobDrivePatchRequest(
            @Pattern(regexp = ".*\\S.*", message = "title must not be blank") String title,
            @Pattern(regexp = ".*\\S.*", message = "location must not be blank") String location,
            @DecimalMin("0.0") @DecimalMax("10.0") Double minimumCgpa,
            @Pattern(regexp = ".*\\S.*", message = "eligibleDepartment must not be blank") String eligibleDepartment,
            LocalDate applicationDeadline,
            LocalDate driveDate,
            String description,
            Set<String> requiredSkills) {
    }

    public record JobDriveResponse(
            Long id,
            Long companyId,
            String companyName,
            String title,
            String location,
            Double minimumCgpa,
            String eligibleDepartment,
            LocalDate applicationDeadline,
            LocalDate driveDate,
            DriveStatus status,
            String description,
            Set<String> requiredSkills) {
    }

    public record EligibilityResponse(Long driveId, boolean eligible) {
    }

    public record DriveReportResponse(
            Long driveId,
            String title,
            long totalApplications,
            Map<ApplicationStatus, Long> statusCounts) {
    }
}
