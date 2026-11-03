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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.college.placement.dto.ApplicationDto.ApplicationRequest;
import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.dto.ApplicationDto.ApplicationStatusRequest;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.security.SecurityUtil;
import com.college.placement.service.ApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Apply, shortlist and track applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Apply to a job drive")
    public ResponseEntity<ApplicationResponse> apply(@Valid @RequestBody ApplicationRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.apply(request.driveId(), auth.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "List applications, optionally filtered by drive and status")
    public ResponseEntity<Page<ApplicationResponse>> list(@RequestParam(required = false) Long driveId,
            @RequestParam(required = false) ApplicationStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(driveId, status, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "My applications")
    public ResponseEntity<Page<ApplicationResponse>> mine(Authentication auth,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.mine(auth.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "Get an application (students can see only their own)")
    public ResponseEntity<ApplicationResponse> get(@PathVariable Long id, Authentication auth) {
        boolean staff = SecurityUtil.hasAnyRole(auth, "ADMIN", "PLACEMENT_OFFICER", "RECRUITER");
        return ResponseEntity.ok(service.get(id, auth.getName(), staff));
    }

    @PatchMapping("/{id}/shortlist")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Shortlist an application")
    public ResponseEntity<ApplicationResponse> shortlist(@PathVariable Long id) {
        return ResponseEntity.ok(service.shortlist(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Reject an application")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ApplicationStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Withdraw my application")
    public ResponseEntity<ApplicationResponse> cancel(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.cancel(id, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a WITHDRAWN or REJECTED application")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
