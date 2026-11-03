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

import com.college.placement.dto.ApplicationDto.OfferRequest;
import com.college.placement.dto.ApplicationDto.OfferRespondRequest;
import com.college.placement.dto.ApplicationDto.OfferResponse;
import com.college.placement.dto.ApplicationDto.OfferUpdateRequest;
import com.college.placement.enums.OfferStatus;
import com.college.placement.security.SecurityUtil;
import com.college.placement.service.OfferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offers")
@Tag(name = "Offers", description = "Issue offers and respond to them")
public class OfferController {

    private final OfferService service;

    public OfferController(OfferService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "Issue an offer")
    public ResponseEntity<OfferResponse> issue(@Valid @RequestBody OfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.issue(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "List offers, optionally filtered by status")
    public ResponseEntity<Page<OfferResponse>> list(@RequestParam(required = false) OfferStatus status,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(status, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "My offers")
    public ResponseEntity<Page<OfferResponse>> mine(Authentication auth,
            @ParameterObject @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.mine(auth.getName(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "Get an offer (students can see only their own)")
    public ResponseEntity<OfferResponse> get(@PathVariable Long id, Authentication auth) {
        boolean staff = SecurityUtil.hasAnyRole(auth, "ADMIN", "PLACEMENT_OFFICER", "RECRUITER");
        return ResponseEntity.ok(service.get(id, auth.getName(), staff));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "Update salary and expiry of an ISSUED offer")
    public ResponseEntity<OfferResponse> update(@PathVariable Long id, @Valid @RequestBody OfferUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/respond")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Accept or reject my offer")
    public ResponseEntity<OfferResponse> respond(@PathVariable Long id, @Valid @RequestBody OfferRespondRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.respond(id, auth.getName(), request.accept()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER','RECRUITER')")
    @Operation(summary = "Withdraw an ISSUED offer")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
