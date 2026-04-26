package com.example.back.Unit.security;

import com.example.back.security.JwtAuthFilter;
import com.example.back.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Be Authorization header tesia filtru grandine")
    void continuesChainWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Kai Authorization nera parasyta Bearer, tesia filtru grandine")
    void continuesChainWhenAuthorizationHeaderIsNotBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Su validziu Bearer token nustato autentifikacija")
    void validBearerTokenSetsAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.parseUserId("valid-token")).thenReturn(10L);
        when(jwtService.parseEmail("valid-token")).thenReturn("user@test.com");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        JwtAuthFilter.UserPrincipal principal = (JwtAuthFilter.UserPrincipal) authentication.getPrincipal();

        assertThat(principal.userId()).isEqualTo(10L);
        assertThat(principal.email()).isEqualTo("user@test.com");
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("Bearer token yra apkarpomas pries parsininima")
    void bearerTokenIsTrimmedBeforeParsing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   valid-token   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.parseUserId("valid-token")).thenReturn(10L);
        when(jwtService.parseEmail("valid-token")).thenReturn("user@test.com");

        filter.doFilter(request, response, chain);

        verify(jwtService).parseUserId("valid-token");
        verify(jwtService).parseEmail("valid-token");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Blogas token grzaina 401 ir netesia grandines")
    void invalidTokenReturnsUnauthorizedAndDoesNotContinueChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.parseUserId("bad-token")).thenThrow(new RuntimeException("bad"));

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Invalid or expired token\"}");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}