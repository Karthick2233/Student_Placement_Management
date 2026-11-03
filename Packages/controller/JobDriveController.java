package com.college.placement.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.college.placement.dto.DriveDto.DriveReportResponse;
import com.college.placement.dto.DriveDto.EligibilityResponse;
import com.college.placement.dto.DriveDto.JobDrivePatchRequest;
import com.college.placement.dto.DriveDto.JobDriveRequest;
import com.college.placement.dto.DriveDto.JobDriveResponse;
import com.college.placement.security.SecurityUtil;
import com.college.placement.service.JobDriveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/job-drives")
@Tag(name = "Job Drives", description = "Job drive APIs")
public class JobDriveController {

    private final JobDriveService service;

    public JobDriveController(JobDriveService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Create a job drive (starts as DRAFT)")
    public ResponseEntity<JobDriveResponse> create(@Valid @RequestBody JobDriveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @Operation(summary = "List published job drives (paged and sorted)")
    public ResponseEntity<Page<JobDriveResponse>> list(
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search published drives by keyword (title or company) and department")
    public ResponseEntity<Page<JobDriveResponse>> search(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String department,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.search(keyword, department, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job drive (non-published drives are visible to staff only)")
    public ResponseEntity<JobDriveResponse> get(@PathVariable Long id, Authentication auth) {
        boolean manager = SecurityUtil.hasAnyRole(auth, "ADMIN", "PLACEMENT_OFFICER");
        return ResponseEntity.ok(service.get(id, manager));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Full update of a DRAFT drive")
    public ResponseEntity<JobDriveResponse> update(@PathVariable Long id, @Valid @RequestBody JobDriveRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Partial update of a DRAFT drive")
    public ResponseEntity<JobDriveResponse> patch(@PathVariable Long id, @Valid @RequestBody JobDrivePatchRequest request) {
        return ResponseEntity.ok(service.patch(id, request));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Publish a drive")
    public ResponseEntity<JobDriveResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(service.publish(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Cancel a drive")
    public ResponseEntity<JobDriveResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Check whether the logged-in student is eligible for the drive")
    public ResponseEntity<EligibilityResponse> eligibility(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.eligibility(id, auth.getName()));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Application count per status for a drive")
    public ResponseEntity<DriveReportResponse> report(@PathVariable Long id) {
        return ResponseEntity.ok(service.report(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Delete a DRAFT or CANCELLED drive")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
