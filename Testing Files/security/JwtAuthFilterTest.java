package com.college.placement.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;

    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesUser() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtService.extractEmail("good-token")).thenReturn("s@example.com");
        when(userDetailsService.loadUserByUsername("s@example.com"))
                .thenReturn(new User("s@example.com", "hash", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        when(jwtService.isValid("good-token", "s@example.com")).thenReturn(true);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("s@example.com", authentication.getName());
        assertEquals("ROLE_STUDENT", authentication.getAuthorities().iterator().next().getAuthority());
        assertNotNull(chain.getRequest());
    }

    @Test
    void requestWithoutHeaderStaysAnonymous() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void invalidTokenDoesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtService.extractEmail("bad-token")).thenReturn("s@example.com");
        when(userDetailsService.loadUserByUsername("s@example.com"))
                .thenReturn(new User("s@example.com", "hash", List.of()));
        when(jwtService.isValid("bad-token", "s@example.com")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void unknownUserDoesNotAuthenticate() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("ghost@example.com");
        when(userDetailsService.loadUserByUsername("ghost@example.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}
