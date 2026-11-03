package com.college.placement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.college.placement.dto.ApplicationDto.ApplicationResponse;
import com.college.placement.enums.ApplicationStatus;
import com.college.placement.security.CustomUserDetailsService;
import com.college.placement.security.JwtService;
import com.college.placement.service.ApplicationService;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationService service;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private static final String APPLY_BODY = "{\"driveId\":1}";

    @Test
    void anonymousUserCannotApply() throws Exception {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "s@example.com", roles = "STUDENT")
    void studentCanApply() throws Exception {
        when(service.apply(1L, "s@example.com"))
                .thenReturn(new ApplicationResponse(1L, 1L, "Ravi", 1L, "Java Developer", ApplicationStatus.APPLIED));

        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotApply() throws Exception {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content(APPLY_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void applyWithoutDriveIdIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotListAllApplications() throws Exception {
        mockMvc.perform(get("/api/applications")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECRUITER")
    void recruiterCanListApplications() throws Exception {
        when(service.list(any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/applications")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCannotShortlist() throws Exception {
        mockMvc.perform(patch("/api/applications/1/shortlist")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PLACEMENT_OFFICER")
    void placementOfficerCanShortlist() throws Exception {
        when(service.shortlist(1L)).thenReturn(
                new ApplicationResponse(1L, 1L, "Ravi", 1L, "Java Developer", ApplicationStatus.SHORTLISTED));

        mockMvc.perform(patch("/api/applications/1/shortlist")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHORTLISTED"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void invalidStateReturnsBadRequest() throws Exception {
        when(service.cancel(any(), any())).thenThrow(new IllegalStateException("Application cannot be cancelled"));

        mockMvc.perform(patch("/api/applications/1/cancel")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Application cannot be cancelled"));
    }
}
