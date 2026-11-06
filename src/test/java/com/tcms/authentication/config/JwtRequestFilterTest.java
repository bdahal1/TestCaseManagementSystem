package com.tcms.authentication.config;

import com.tcms.authentication.service.CustomUserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import jakarta.servlet.FilterChain;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtRequestFilterTest {
    JwtTokenUtil tokens = mock(JwtTokenUtil.class);
    CustomUserDetailService users = mock(CustomUserDetailService.class);
    JwtRequestFilter filter = new JwtRequestFilter(tokens, users);
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void expiredTokenReturns401AndStopsTheChain() throws Exception {
        when(tokens.getUsernameFromToken("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }
    @Test void disabledUserCannotReuseValidToken() throws Exception {
        when(tokens.getUsernameFromToken("valid")).thenReturn("disabled");
        when(users.loadUserByUsername("disabled")).thenReturn(new User("disabled", "hash", false, true, true, true, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }
    @Test void publicRequestWithoutTokenContinues() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(); MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class); filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test void validTokenAuthenticatesAndContinues() throws Exception {
        var user = new User("engineer", "hash", List.of());
        when(tokens.getUsernameFromToken("valid")).thenReturn("engineer");
        when(users.loadUserByUsername("engineer")).thenReturn(user);
        when(tokens.validateToken("valid", user)).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer valid");
        MockHttpServletResponse response = new MockHttpServletResponse(); FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("engineer");
        verify(chain).doFilter(request, response);
    }
    @Test void refreshErrorsNeverContainTheToken() {
        var error = new com.tcms.authentication.service.TokenRefreshException("private-token", "Expired session");
        assertThat(error.getMessage()).doesNotContain("private-token");
    }
}
