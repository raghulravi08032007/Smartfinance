package com.rawgul.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point
 * 
 * This component handles authentication errors and returns standardized JSON error responses
 * when users try to access protected resources without valid authentication.
 * 
 * Triggered when:
 * - No JWT token provided in Authorization header
 * - Invalid JWT token (expired, malformed, invalid signature)
 * - User not found or account disabled
 * 
 * Returns:
 * - HTTP 401 Unauthorized
 * - JSON error response with details
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
@Component
@Slf4j
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public AuthEntryPointJwt() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Handles authentication exceptions by returning a JSON error response.
     * 
     * @param request HTTP request that resulted in authentication exception
     * @param response HTTP response to write error details
     * @param authException the authentication exception that was thrown
     * @throws IOException if I/O error occurs
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void commence(
            HttpServletRequest request, 
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        String errorMessage = authException.getMessage();
        
        // Log the authentication failure with details
        log.error("Authentication failed for {} {} - IP: {} - Error: {}", 
            method, 
            requestPath,
            getClientIpAddress(request),
            errorMessage
        );
        
        // Set response headers
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        
        // Build error response body
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("message", getUserFriendlyMessage(errorMessage));
        body.put("path", requestPath);
        body.put("timestamp", LocalDateTime.now());
        
        // Add hint for missing token
        if (errorMessage != null && errorMessage.contains("Full authentication is required")) {
            body.put("hint", "Please provide a valid JWT token in the Authorization header (Bearer <token>)");
        }
        
        // Write JSON response
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Converts technical error messages to user-friendly messages.
     * 
     * @param errorMessage technical error message
     * @return user-friendly error message
     */
    private String getUserFriendlyMessage(String errorMessage) {
        if (errorMessage == null) {
            return "Authentication required to access this resource";
        }
        
        // Convert common technical messages to user-friendly ones
        if (errorMessage.contains("Full authentication is required")) {
            return "Authentication required. Please log in to access this resource.";
        } else if (errorMessage.contains("Access is denied")) {
            return "Access denied. You do not have permission to access this resource.";
        } else if (errorMessage.contains("Bad credentials")) {
            return "Invalid username or password.";
        } else if (errorMessage.contains("User is disabled")) {
            return "Your account has been disabled. Please contact support.";
        } else if (errorMessage.contains("User account is locked")) {
            return "Your account has been locked. Please contact support or reset your password.";
        } else if (errorMessage.contains("JWT expired")) {
            return "Your session has expired. Please log in again.";
        } else if (errorMessage.contains("JWT signature")) {
            return "Invalid authentication token. Please log in again.";
        }
        
        // Return original message if no specific mapping found
        return errorMessage;
    }

    /**
     * Extracts the client's IP address from the request.
     * Checks various headers that proxies/load balancers might set.
     * 
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }

}
