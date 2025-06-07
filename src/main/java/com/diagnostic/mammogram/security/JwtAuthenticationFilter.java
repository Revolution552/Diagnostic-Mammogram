package com.diagnostic.mammogram.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader(AUTH_HEADER);

            if (!isBearerTokenPresent(authHeader)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = extractJwtFromHeader(authHeader);
            authenticateRequest(jwt, request);

        } catch (JwtException | UsernameNotFoundException ex) {
            handleAuthenticationError(response, ex);
            return;
        } catch (Exception ex) {
            log.error("Unexpected authentication error", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBearerTokenPresent(String authHeader) {
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    private String extractJwtFromHeader(String authHeader) {
        return authHeader.substring(BEARER_PREFIX_LENGTH);
    }

    private void authenticateRequest(String jwt, HttpServletRequest request) {
        final String username = jwtService.extractUsername(jwt);

        if (username != null && isContextNotAuthenticated()) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                setSecurityContextAuthentication(userDetails, request);
                log.debug("Authenticated user: {}", username);
            }
        }
    }

    private boolean isContextNotAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() == null;
    }

    private void setSecurityContextAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception ex) throws IOException {
        log.warn("Authentication failed: {}", ex.getMessage());

        if (ex instanceof JwtException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        } else if (ex instanceof UsernameNotFoundException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
        }
    }
}