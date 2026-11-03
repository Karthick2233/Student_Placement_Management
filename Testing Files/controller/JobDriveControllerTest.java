package com.college.placement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.college.placement.config.SecurityConfig;
import com.college.placement.dto.DriveDto.EligibilityResponse;
import com.college.placement.dto.DriveDto.JobDriveRequest;
import com.college.placement.dto.DriveDto.JobDriveResponse;
import com.college.placement.enums.DriveStatus;
import com.college.placement.exception.NotFoundException;
import com.college.placement.security.CustomUserDetailsService;
import com.college.placement.security.JwtService;
import com.college.placement.service.JobDriveService;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(JobDriveController.class)
@Import(SecurityConfig.class)
class JobDriveControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JobDriveService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private String validBody() throws Exception {
        JobDriveRequest request = new JobDriveRequest(1L, "Java Developer", "Chennai", 6.0, "ALL",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10), "Backend", Set.of("Java"));
        return objectMapper.writeValueAsString(request);
    }

    private JobDriveResponse response() {
        return new JobDriveResponse(1L, 1L, "Acme", "Java Developer", "Chennai", 6.0, "ALL",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(10), DriveStatus.DRAFT, "Backend",
                Set.of("Java"));
    }

    @Test
    void anyoneCanListPublishedDrives() throws Exception {
        when(service.list(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/job-drives")).andExpect(status().isOk());
    }

    @Test
    void anonymousUserCannotCreateDrive() throws Exception {
        mockMvc.perform(post("/api/job-drives").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotCreateDrive() throws Exception {
        mockMvc.perform(post("/api/job-drives").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLACEMENT_OFFICER")
    void placementOfficerCanCreateDrive() throws Exception {
        when(service.create(any(JobDriveRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/job-drives").contentType(MediaType.APPLICATION_JSON).content(validBody()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Java Developer"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @WithMockUser(roles = "PLACEMENT_OFFICER")
    void invalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/job-drives").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void draftDriveIsNotFoundForPublicUser() throws Exception {
        when(service.get(5L, false)).thenThrow(new NotFoundException("Job drive not found"));

        mockMvc.perform(get("/api/job-drives/5")).andExpect(status().isNotFound());
    }

    @Test
    void eligibilityNeedsLogin() throws Exception {
        mockMvc.perform(get("/api/job-drives/1/eligibility")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotCheckEligibility() throws Exception {
        mockMvc.perform(get("/api/job-drives/1/eligibility")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void studentCanCheckEligibility() throws Exception {
        when(service.eligibility(1L, "s@example.com")).thenReturn(new EligibilityResponse(1L, true));

        mockMvc.perform(get("/api/job-drives/1/eligibility")).andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotSeeDriveReport() throws Exception {
        mockMvc.perform(get("/api/job-drives/1/report")).andExpect(status().isForbidden());
    }
}
