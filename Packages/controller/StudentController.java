package com.college.placement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.college.placement.dto.StudentDto.DepartmentStatResponse;
import com.college.placement.dto.StudentDto.FileUploadResponse;
import com.college.placement.service.StudentQueryService;
import com.college.placement.service.StudentResumeService;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.dto.StudentDto.StudentPatchRequest;
import com.college.placement.dto.StudentDto.StudentRequest;
import com.college.placement.dto.StudentDto.StudentResponse;
import com.college.placement.security.SecurityUtil;
import com.college.placement.service.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students", description = "Student profile APIs")
public class StudentController {

    private final StudentService service;
    private final StudentResumeService resumeService;
    private final StudentQueryService queryService;

    public StudentController(StudentService service, StudentResumeService resumeService,
            StudentQueryService queryService) {
        this.service = service;
        this.resumeService = resumeService;
        this.queryService = queryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create own student profile")
    public ResponseEntity<StudentResponse> create(@Valid @RequestBody StudentRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(auth.getName(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "List students (paged and sorted)")
    public ResponseEntity<Page<StudentResponse>> list(@ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Search students by department, minimum CGPA and graduation year")
    public ResponseEntity<Page<StudentResponse>> search(@RequestParam(required = false) String department,
            @RequestParam(required = false) Double minCgpa, @RequestParam(required = false) Integer graduationYear,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.search(department, minCgpa, graduationYear, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Get a student by id (students can see only their own profile)")
    public ResponseEntity<StudentResponse> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(service.get(id, auth.getName(), isStaff(auth)));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Application history of a student")
    public ResponseEntity<List<ApplicationResponse>> history(@PathVariable Long id) {
        return ResponseEntity.ok(service.history(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Full update of a student profile")
    public ResponseEntity<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.update(id, request, auth.getName(), isStaff(auth)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Partial update of a student profile")
    public ResponseEntity<StudentResponse> patch(@PathVariable Long id, @Valid @RequestBody StudentPatchRequest request,
            Authentication auth) {
        return ResponseEntity.ok(service.patch(id, request, auth.getName(), isStaff(auth)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a student profile")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Upload a resume (pdf, doc or docx, maximum 5MB)")
    public ResponseEntity<FileUploadResponse> uploadResume(@PathVariable Long id,
            @RequestParam("file") MultipartFile file, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resumeService.upload(id, file, auth.getName(), isStaff(auth)));
    }

    @GetMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Download the resume")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long id, Authentication auth) {
        Resource resource = resumeService.download(id, auth.getName(), isStaff(auth));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(resource.getFilename() == null ? "resume" : resource.getFilename()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(resource);
    }

    @DeleteMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Delete the resume")
    public ResponseEntity<Void> deleteResume(@PathVariable Long id, Authentication auth) {
        resumeService.delete(id, auth.getName(), isStaff(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/queries/jpql/top-by-department")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "JPQL: students of a department, best CGPA first")
    public ResponseEntity<List<StudentResponse>> topByDepartment(@RequestParam String department) {
        return ResponseEntity.ok(queryService.topStudentsOfDepartment(department));
    }

    @GetMapping("/queries/jpql/by-skill")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "JPQL: students having a skill")
    public ResponseEntity<List<StudentResponse>> bySkill(@RequestParam String skill) {
        return ResponseEntity.ok(queryService.bySkill(skill));
    }

    @GetMapping("/queries/jpql/applied-to-drive/{driveId}")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "JPQL: students who applied to a drive")
    public ResponseEntity<List<StudentResponse>> appliedToDrive(@PathVariable Long driveId) {
        return ResponseEntity.ok(queryService.appliedToDrive(driveId));
    }

    @GetMapping("/queries/jpql/department-stats")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "JPQL (cached): student count and average CGPA per department")
    public ResponseEntity<List<DepartmentStatResponse>> departmentStats() {
        return ResponseEntity.ok(queryService.departmentStats());
    }

    @GetMapping("/queries/native/min-cgpa")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Native SQL: students with CGPA at or above a value")
    public ResponseEntity<List<StudentResponse>> nativeMinCgpa(@RequestParam(defaultValue = "8.0") double value) {
        return ResponseEntity.ok(queryService.byMinCgpaNative(value));
    }

    @GetMapping("/queries/native/without-applications")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Native SQL: students who never applied")
    public ResponseEntity<List<StudentResponse>> withoutApplications() {
        return ResponseEntity.ok(queryService.withoutApplications());
    }

    @GetMapping("/queries/native/department-summary")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Native SQL: GROUP BY summary per department")
    public ResponseEntity<List<DepartmentStatResponse>> departmentSummary() {
        return ResponseEntity.ok(queryService.departmentSummaryNative());
    }

    @GetMapping("/queries/named/placed")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Named query: students with an ACCEPTED offer")
    public ResponseEntity<List<StudentResponse>> placed() {
        return ResponseEntity.ok(queryService.placedStudents());
    }

    @GetMapping("/queries/named/min-cgpa")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Named query: students with CGPA at or above a value")
    public ResponseEntity<List<StudentResponse>> namedMinCgpa(@RequestParam Double value) {
        return ResponseEntity.ok(queryService.byMinCgpaNamed(value));
    }

    @GetMapping("/queries/custom/search")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Custom (Criteria API): dynamic search, every filter is optional")
    public ResponseEntity<List<StudentResponse>> customSearch(@RequestParam(required = false) String department,
            @RequestParam(required = false) Double minCgpa, @RequestParam(required = false) Integer graduationYear,
            @RequestParam(required = false) String skill) {
        return ResponseEntity.ok(queryService.customSearch(department, minCgpa, graduationYear, skill));
    }

    @GetMapping("/queries/custom/average-cgpa")
    @PreAuthorize("hasAnyRole('ADMIN','PLACEMENT_OFFICER')")
    @Operation(summary = "Custom: average CGPA (all students, or one department)")
    public ResponseEntity<Map<String, Double>> averageCgpa(@RequestParam(required = false) String department) {
        return ResponseEntity.ok(Map.of("averageCgpa", queryService.averageCgpa(department)));
    }

    private boolean isStaff(Authentication auth) {
        return SecurityUtil.hasAnyRole(auth, "ADMIN", "PLACEMENT_OFFICER");
    }
}
