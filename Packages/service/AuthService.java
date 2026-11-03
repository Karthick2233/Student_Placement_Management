package com.college.placement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.dto.AuthDto.AuthResponse;
import com.college.placement.dto.AuthDto.LoginRequest;
import com.college.placement.dto.AuthDto.RegisterRequest;
import com.college.placement.entity.AppUser;
import com.college.placement.enums.Role;
import com.college.placement.exception.ConflictException;
import com.college.placement.exception.NotFoundException;
import com.college.placement.repository.UserRepository;
import com.college.placement.security.JwtService;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        AppUser user = users.save(new AppUser(request.name().trim(), email, encoder.encode(request.password()),
                Role.STUDENT));
        log.info("Student account registered: {}", email);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        AppUser user = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        log.info("User logged in: {}", email);
        return toResponse(user);
    }

    private AuthResponse toResponse(AppUser user) {
        String role = user.getRole().name();
        return new AuthResponse(jwtService.generate(user.getEmail(), role), "Bearer", user.getEmail(), role);
    }
}
