package com.college.placement.dto;

import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class StudentDto {

    private StudentDto() {
    }

    public record StudentRequest(
            @NotBlank String department,
            @NotNull @Min(2020) Integer graduationYear,
            @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double cgpa,
            Set<String> skills) {
    }

    public record StudentPatchRequest(
            @Pattern(regexp = ".*\\S.*", message = "department must not be blank") String department,
            @Min(2020) Integer graduationYear,
            @DecimalMin("0.0") @DecimalMax("10.0") Double cgpa,
            Set<String> skills) {
    }

    public record StudentResponse(
            Long id,
            String name,
            String email,
            String department,
            Integer graduationYear,
            Double cgpa,
            Set<String> skills) {
    }

    public record DepartmentStatResponse(String department, Long studentCount, Double averageCgpa) {
    }

    public record FileUploadResponse(String fileName, long size, String downloadUrl) {
    }
}
