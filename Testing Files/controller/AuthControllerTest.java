package com.college.placement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.college.placement.config.SecurityConfig;
import com.college.placement.dto.AuthDto.AuthResponse;
import com.college.placement.dto.AuthDto.LoginRequest;
import com.college.placement.dto.AuthDto.RegisterRequest;
import com.college.placement.exception.ConflictException;
import com.college.placement.security.CustomUserDetailsService;
import com.college.placement.security.JwtService;
import com.college.placement.service.AuthService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "Bearer", "admin@example.com", "ADMIN"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"AdminPass123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void registerCreatesStudentAccount() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("jwt-token", "Bearer", "new@example.com", "STUDENT"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"email\":\"new@example.com\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"email\":\"new@example.com\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenThrow(new ConflictException("Email already registered"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\",\"email\":\"used@example.com\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isConflict());
    }
}
