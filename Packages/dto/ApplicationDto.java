package com.college.placement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.college.placement.enums.ApplicationStatus;
import com.college.placement.enums.InterviewResult;
import com.college.placement.enums.OfferStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class ApplicationDto {

    private ApplicationDto() {
    }

    public record ApplicationRequest(@NotNull Long driveId) {
    }

    public record ApplicationStatusRequest(@NotNull ApplicationStatus status) {
    }

    public record ApplicationResponse(
            Long id,
            Long studentId,
            String studentName,
            Long driveId,
            String driveTitle,
            ApplicationStatus status) {
    }

    public record InterviewRequest(
            @NotNull Long applicationId,
            @NotNull @Future LocalDateTime scheduledAt,
            @NotBlank String roundName) {
    }

    public record InterviewUpdateRequest(
            @NotNull @Future LocalDateTime scheduledAt,
            @NotBlank String roundName) {
    }

    public record InterviewResultRequest(
            @NotNull InterviewResult result,
            @Size(max = 1000) String feedback) {
    }

    public record InterviewResponse(
            Long id,
            Long applicationId,
            LocalDateTime scheduledAt,
            String roundName,
            InterviewResult result,
            String feedback) {
    }

    public record OfferRequest(
            @NotNull Long applicationId,
            @NotNull @Positive Double salary,
            @NotNull LocalDate expiryDate) {
    }

    public record OfferUpdateRequest(
            @NotNull @Positive Double salary,
            @NotNull LocalDate expiryDate) {
    }

    public record OfferRespondRequest(@NotNull Boolean accept) {
    }

    public record OfferResponse(
            Long id,
            Long applicationId,
            String studentName,
            String driveTitle,
            Double salary,
            LocalDate expiryDate,
            OfferStatus status) {
    }
}
