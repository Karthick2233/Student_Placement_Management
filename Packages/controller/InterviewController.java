package com.college.placement.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.placement.dto.ApplicationDto.InterviewRequest;
import com.college.placement.dto.ApplicationDto.InterviewResponse;
import com.college.placement.dto.ApplicationDto.InterviewResultRequest;
import com.college.placement.dto.ApplicationDto.InterviewUpdateRequest;
import com.college.placement.enums.InterviewResult;
import com.college.placement.service.InterviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Interviews", description = "Schedule interviews and update results")
@PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
public class InterviewController {

    private final InterviewService service;

    public InterviewController(InterviewService service) {
        this.service = service;
    }

    @PostMapping("/interviews")
    @Operation(summary = "Schedule an interview")
    public ResponseEntity<InterviewResponse> schedule(@Valid @RequestBody InterviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.schedule(request));
    }

    @GetMapping("/interviews")
    @Operation(summary = "List interviews, optionally filtered by application and result")
    public ResponseEntity<Page<InterviewResponse>> list(@RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) InterviewResult result,
            @ParameterObject @PageableDefault(size = 10, sort = "scheduledAt") Pageable pageable) {
        return ResponseEntity.ok(service.list(applicationId, result, pageable));
    }

    @GetMapping("/interviews/{id}")
    @Operation(summary = "Get an interview by id")
    public ResponseEntity<InterviewResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/applications/{applicationId}/interviews")
    @Operation(summary = "Interview history of an application")
    public ResponseEntity<List<InterviewResponse>> byApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(service.byApplication(applicationId));
    }

    @PutMapping("/interviews/{id}")
    @Operation(summary = "Reschedule an interview")
    public ResponseEntity<InterviewResponse> update(@PathVariable Long id, @Valid @RequestBody InterviewUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/interviews/{id}/result")
    @Operation(summary = "Update the interview result")
    public ResponseEntity<InterviewResponse> updateResult(@PathVariable Long id, @Valid @RequestBody InterviewResultRequest request) {
        return ResponseEntity.ok(service.updateResult(id, request));
    }

    @DeleteMapping("/interviews/{id}")
    @Operation(summary = "Delete a scheduled interview")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
