package com.threatpulse.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter responsible for JWT-based authentication.
 * Extracts the Bearer token from the Authorization header, validates it,
 * and establishes the authenticated user in the SecurityContext.
 * If the token is valid, a UsernamePasswordAuthenticationToken is created
 * and set in the context, enabling authorization for secured endpoints.
 * This filter runs once per request and is part of the Spring Security filter chain.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Extract authorization header and check it
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token and user username
        String token = header.substring(7);
        String email = jwtService.extractUsername(token);

        // Check if username is empty and user is not already authenticated
        if (!email.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null){

            // Load user details from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Validate token against user details
            if (jwtService.isTokenValid(token, userDetails)) {

                // Create authentication token
                UsernamePasswordAuthenticationToken authToken = new
                        UsernamePasswordAuthenticationToken(userDetails, null,
                        userDetails.getAuthorities());

                // Attach request details (IP, session...)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
