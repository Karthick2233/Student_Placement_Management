package com.college.placement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.college.placement.entity.AppUser;
import com.college.placement.enums.Role;
import com.college.placement.repository.UserRepository;

class CustomUserDetailsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    void loadsUserWithRolePrefix() {
        when(users.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(new AppUser("Admin", "admin@example.com", "hash", Role.ADMIN)));

        UserDetails details = service.loadUserByUsername("admin@example.com");

        assertEquals("admin@example.com", details.getUsername());
        assertEquals("hash", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void unknownEmailThrowsException() {
        when(users.findByEmail("none@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("none@example.com"));
    }
}
