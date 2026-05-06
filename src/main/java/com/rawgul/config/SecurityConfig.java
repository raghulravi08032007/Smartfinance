package com.rawgul.config;

import com.rawgul.security.AuthEntryPointJwt;
import com.rawgul.security.JwtAuthenticationFilter;
import com.rawgul.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Comprehensive Spring Security Configuration
 * 
 * Features:
 * - JWT-based authentication with stateless sessions
 * - BCrypt password encoding with strength 12
 * - CORS configuration for frontend integration
 * - Public endpoints for authentication and password reset
 * - Protected endpoints requiring authentication
 * - Method-level security with @PreAuthorize
 * - Custom authentication entry point for 401 errors
 * - CSRF protection disabled (stateless JWT approach)
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://127.0.0.1:5500}")
    private String allowedOrigins;

    @Value("${app.security.bcrypt-strength:12}")
    private int bcryptStrength;

    /**
     * Configures the authentication provider with custom UserDetailsService and password encoder.
     * 
     * @return configured DaoAuthenticationProvider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false); // For better error messages
        return authProvider;
    }

    /**
     * Exposes the authentication manager bean for use in controllers.
     * 
     * @param authConfig authentication configuration
     * @return AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Password encoder using BCrypt algorithm with configurable strength.
     * Default strength is 12 (2^12 rounds).
     * 
     * @return BCryptPasswordEncoder with configured strength
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * CORS configuration to allow frontend applications to access the API.
     * 
     * Allowed origins can be configured via application.properties:
     * app.cors.allowed-origins=http://localhost:3000,http://localhost:8080
     * 
     * @return CorsConfigurationSource with configured settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allow common headers including Authorization for JWT
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Cache-Control"
        ));
        
        // Expose headers that frontend can access
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Main security filter chain configuration.
     * 
     * Public Endpoints (No Authentication Required):
     * - /api/auth/** - Authentication endpoints (login, register)
     * - /api/password-reset/** - Password reset workflow
     * - /api/support/tickets (POST) - Public support ticket creation
     * - /api/support/ticket/{ticketNumber} (GET) - Public ticket status check
     * - Static resources (HTML, CSS, JS, images)
     * - Actuator endpoints (for monitoring)
     * 
     * Protected Endpoints (Authentication Required):
     * - /api/users/** - User management (role-based access)
     * - /api/support/tickets (GET) - User's tickets or all tickets (admin)
     * - All other /api/** endpoints
     * 
     * Security Features:
     * - CSRF disabled (using stateless JWT tokens)
     * - Stateless session management (no server-side sessions)
     * - JWT filter before UsernamePasswordAuthenticationFilter
     * - Custom authentication entry point for 401 errors
     * - CORS enabled for frontend integration
     * 
     * @param http HttpSecurity to configure
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS configuration - enable for frontend integration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // CSRF protection - disabled for stateless JWT authentication
            // Note: Enable if using cookie-based authentication
            .csrf(AbstractHttpConfigurer::disable)
            
            // Exception handling - custom entry point for authentication errors
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(unauthorizedHandler)
            )
            
            // Session management - stateless (no server-side sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/signup").permitAll() // Legacy endpoint
                
                // Public password reset endpoints
                .requestMatchers("/api/password-reset/**").permitAll()
                .requestMatchers("/api/password-reset/request").permitAll()
                .requestMatchers("/api/password-reset/forgot-password").permitAll() // Legacy
                .requestMatchers("/api/password-reset/validate").permitAll()
                .requestMatchers("/api/password-reset/reset").permitAll()
                .requestMatchers("/api/password-reset/reset-password").permitAll() // Legacy
                
                // Public support ticket endpoints
                .requestMatchers("/api/support/tickets").permitAll() // POST only (create ticket)
                .requestMatchers("/api/support/ticket").permitAll() // Legacy POST endpoint
                .requestMatchers("/api/support/ticket/{ticketNumber}").permitAll() // GET by ticket number
                
                // Actuator endpoints for monitoring (consider restricting in production)
                .requestMatchers("/actuator/**").permitAll()
                
                // Static resources (HTML pages, CSS, JavaScript, images)
                .requestMatchers("/", "/index.html").permitAll()
                .requestMatchers("/login.html", "/sign-up.html", "/forgot-password.html").permitAll()
                .requestMatchers("/support.html").permitAll()
                .requestMatchers("/*.html", "/*.css", "/*.js").permitAll()
                .requestMatchers("/assets/**", "/favicon_io/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );

        // Set custom authentication provider
        http.authenticationProvider(authenticationProvider());
        
        // Add JWT filter before standard authentication filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
