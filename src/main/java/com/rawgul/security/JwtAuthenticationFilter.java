package com.rawgul.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * This filter intercepts every request to validate JWT tokens and establish Spring Security context.
 * 
 * Flow:
 * 1. Extract JWT token from Authorization header (Bearer token)
 * 2. Validate token signature and expiration
 * 3. Extract username from token claims
 * 4. Load user details from database
 * 5. Create authentication object and set in SecurityContext
 * 
 * Features:
 * - Runs once per request (OncePerRequestFilter)
 * - Handles token parsing from Authorization header
 * - Validates token before setting authentication
 * - Graceful error handling (logs errors but allows request to continue)
 * - Extracts request details (IP, user agent) for audit
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Main filter method that processes each HTTP request.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain to continue processing
     * @throws ServletException if servlet error occurs
     * @throws IOException if I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        try {
            // Extract JWT token from request header
            String jwt = parseJwt(request);
            
            // Validate token and set authentication
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                log.debug("JWT token validated for user: {} on path: {}", username, requestPath);
                
                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Create authentication token with user details and authorities
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, 
                        null, 
                        userDetails.getAuthorities()
                    );
                
                // Set authentication details (IP address, session ID, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in Spring Security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("Security context set for user: {} with authorities: {}", 
                    username, userDetails.getAuthorities());
            } else if (jwt != null) {
                log.warn("Invalid JWT token on path: {}", requestPath);
            }
            
        } catch (UsernameNotFoundException e) {
            log.error("User not found in database: {} on path: {}", e.getMessage(), requestPath);
        } catch (Exception e) {
            log.error("Cannot set user authentication on path: {} - Error: {}", 
                requestPath, e.getMessage(), e);
        }
        
        // Always continue the filter chain
        // If authentication failed, SecurityContext will be empty and endpoints will return 401
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from Authorization header.
     * 
     * Expected header format: Authorization: Bearer <jwt-token>
     * 
     * @param request HTTP request
     * @return JWT token string or null if not found or invalid format
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        if (StringUtils.hasText(headerAuth)) {
            // Check for Bearer token format
            if (headerAuth.startsWith("Bearer ")) {
                String token = headerAuth.substring(7); // Remove "Bearer " prefix
                
                if (StringUtils.hasText(token)) {
                    log.debug("JWT token extracted from Authorization header");
                    return token;
                } else {
                    log.warn("Empty JWT token after Bearer prefix");
                }
            } else {
                log.warn("Authorization header does not start with Bearer: {}", 
                    headerAuth.substring(0, Math.min(20, headerAuth.length())));
            }
        }
        
        return null;
    }
    
    /**
     * Determines if this filter should be applied to the request.
     * Can be overridden to skip filtering for certain paths (e.g., static resources).
     * 
     * @param request HTTP request
     * @return true to skip filtering, false to apply filter
     * @throws ServletException if error occurs
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip JWT validation for public static resources
        return path.endsWith(".css") || 
               path.endsWith(".js") || 
               path.endsWith(".png") || 
               path.endsWith(".jpg") || 
               path.endsWith(".ico") ||
               path.startsWith("/assets/") ||
               path.startsWith("/favicon_io/");
    }

}
