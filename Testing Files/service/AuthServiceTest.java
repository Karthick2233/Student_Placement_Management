package com.college.placement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.college.placement.dto.AuthDto.AuthResponse;
import com.college.placement.dto.AuthDto.LoginRequest;
import com.college.placement.dto.AuthDto.RegisterRequest;
import com.college.placement.entity.AppUser;
import com.college.placement.enums.Role;
import com.college.placement.exception.ConflictException;
import com.college.placement.repository.UserRepository;
import com.college.placement.security.JwtService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, authenticationManager, jwtService);
    }

    @Test
    void registerCreatesStudentWithLowercaseEmail() {
        when(users.existsByEmail("new@example.com")).thenReturn(false);
        when(encoder.encode("StrongPass123")).thenReturn("hashed");
        when(users.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generate("new@example.com", "STUDENT")).thenReturn("token");

        AuthResponse response = service.register(new RegisterRequest("New User", "New@Example.com", "StrongPass123"));

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertEquals(Role.STUDENT, saved.getValue().getRole());
        assertEquals("new@example.com", saved.getValue().getEmail());
        assertEquals("hashed", saved.getValue().getPasswordHash());
        assertEquals("token", response.token());
        assertEquals("Bearer", response.tokenType());
    }

    @Test
    void registerFailsForExistingEmail() {
        when(users.existsByEmail("used@example.com")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.register(new RegisterRequest("User", "used@example.com", "StrongPass123")));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        AppUser user = new AppUser("Admin", "admin@example.com", "hash", Role.ADMIN);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generate("admin@example.com", "ADMIN")).thenReturn("admin-token");

        AuthResponse response = service.login(new LoginRequest("admin@example.com", "AdminPass123"));

        assertEquals("admin-token", response.token());
        assertEquals("ADMIN", response.role());
    }

    @Test
    void loginFailsForBadCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginRequest("admin@example.com", "wrong")));
        verify(users, never()).findByEmail(any());
    }
}
