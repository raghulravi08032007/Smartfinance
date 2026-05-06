package com.rawgul.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Token Provider (Utility Class)
 * 
 * Comprehensive JWT token management utility for:
 * - Token generation (access and refresh tokens)
 * - Token validation and verification
 * - Claims extraction (username, expiration, custom claims)
 * - Token expiration checking
 * - Refresh token logic
 * 
 * Features:
 * - HMAC-SHA256 signing algorithm
 * - Configurable secret key from application.properties
 * - Configurable expiration times (access and refresh tokens)
 * - Role-based claims in token payload
 * - Comprehensive error handling and logging
 * - Token refresh capability
 * 
 * Configuration Properties:
 * - jwt.secret: Secret key for signing (min 256 bits)
 * - jwt.expiration: Access token expiration (milliseconds)
 * - jwt.refresh.expiration: Refresh token expiration (milliseconds)
 * 
 * @author Raghul
 * @version 2.0
 * @since 2025-11-03
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}") // Default: 24 hours
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration:604800000}") // Default: 7 days
    private long jwtRefreshExpirationMs;

    // Token type claim key
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    
    // Roles claim key
    private static final String ROLES_CLAIM = "roles";

    /**
     * Gets the signing key for JWT tokens.
     * Uses HMAC-SHA256 algorithm with the configured secret.
     * 
     * @return SecretKey for signing/verifying tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access JWT token from Spring Security Authentication.
     * 
     * Token includes:
     * - Subject: username
     * - Issued At: current timestamp
     * - Expiration: configured expiration time
     * - Roles: user's granted authorities
     * - Token Type: "access"
     * 
     * @param authentication Spring Security authentication object
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromUsername(
            userPrincipal.getUsername(), 
            userPrincipal.getId(),
            authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList())
        );
    }

    /**
     * Generates an access JWT token from username, user ID, and roles.
     * 
     * @param username username to include in token
     * @param userId user ID to include in token
     * @param roles list of role names
     * @return JWT token string
     */
    public String generateTokenFromUsername(String username, Long userId, java.util.List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(ROLES_CLAIM, roles);
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);

        log.debug("Generating access token for user: {} with expiration: {}", username, expiryDate);

        return Jwts.builder()
            .subject(username)
            .claims(claims)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Generates a refresh JWT token for token renewal.
     * 
     * Refresh tokens have longer expiration time and are used to obtain new access tokens
     * without requiring the user to log in again.
     * 
     * @param authentication Spring Security authentication object
     * @return Refresh token string
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateRefreshTokenFromUsername(userPrincipal.getUsername(), userPrincipal.getId());
    }

    /**
     * Generates a refresh token from username and user ID.
     * 
     * @param username username to include in token
     * @param userId user ID to include in token
     * @return Refresh token string
     */
    public String generateRefreshTokenFromUsername(String username, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);

        log.debug("Generating refresh token for user: {} with expiration: {}", username, expiryDate);

        return Jwts.builder()
            .subject(username)
            .claims(claims)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extracts username from JWT token.
     * 
     * @param token JWT token string
     * @return username (subject claim)
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Extracts user ID from JWT token.
     * 
     * @param token JWT token string
     * @return user ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * Extracts roles from JWT token.
     * 
     * @param token JWT token string
     * @return list of role names
     */
    @SuppressWarnings("unchecked")
    public java.util.List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (java.util.List<String>) claims.get(ROLES_CLAIM);
    }

    /**
     * Extracts expiration date from JWT token.
     * 
     * @param token JWT token string
     * @return expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Extracts issued at date from JWT token.
     * 
     * @param token JWT token string
     * @return issued at date
     */
    public Date getIssuedAtDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuedAt();
    }

    /**
     * Checks if the token type is an access token.
     * 
     * @param token JWT token string
     * @return true if access token, false otherwise
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return ACCESS_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if the token type is a refresh token.
     * 
     * @param token JWT token string
     * @return true if refresh token, false otherwise
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if JWT token is expired.
     * 
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            log.debug("Token is expired: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Gets remaining time until token expires.
     * 
     * @param token JWT token string
     * @return remaining milliseconds until expiration, or 0 if expired
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            long remaining = expiration.getTime() - new Date().getTime();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Validates JWT token.
     * 
     * Checks:
     * - Token signature is valid
     * - Token is not expired
     * - Token is not malformed
     * - Claims are valid
     * 
     * @param authToken JWT token string to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken);
            
            log.debug("Token validation successful");
            return true;
            
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT token validation error: {}", e.getMessage());
        }
        
        return false;
    }

    /**
     * Validates refresh token specifically.
     * Checks both general validity and token type.
     * 
     * @param refreshToken refresh token string
     * @return true if valid refresh token, false otherwise
     */
    public boolean validateRefreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            return false;
        }
        
        if (!isRefreshToken(refreshToken)) {
            log.error("Token is not a refresh token");
            return false;
        }
        
        return true;
    }

    /**
     * Refreshes an access token using a valid refresh token.
     * 
     * @param refreshToken valid refresh token
     * @param roles user's current roles (may have changed since refresh token issued)
     * @return new access token
     * @throws IllegalArgumentException if refresh token is invalid
     */
    public String refreshAccessToken(String refreshToken, java.util.List<String> roles) {
        if (!validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        
        String username = getUsernameFromToken(refreshToken);
        Long userId = getUserIdFromToken(refreshToken);
        
        log.info("Refreshing access token for user: {}", username);
        
        return generateTokenFromUsername(username, userId, roles);
    }

    /**
     * Extracts all claims from JWT token.
     * 
     * @param token JWT token string
     * @return Claims object containing all token claims
     * @throws JwtException if token parsing fails
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Gets the configured access token expiration time in milliseconds.
     * 
     * @return expiration time in milliseconds
     */
    public long getExpirationTime() {
        return jwtExpirationMs;
    }

    /**
     * Gets the configured refresh token expiration time in milliseconds.
     * 
     * @return refresh token expiration time in milliseconds
     */
    public long getRefreshExpirationTime() {
        return jwtRefreshExpirationMs;
    }

    /**
     * Creates a human-readable summary of token information.
     * Useful for debugging and logging.
     * 
     * @param token JWT token string
     * @return formatted string with token details
     */
    public String getTokenInfo(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            
            return String.format(
                "Token Info - Type: %s, Subject: %s, Issued: %s, Expires: %s, Expired: %s",
                tokenType,
                claims.getSubject(),
                claims.getIssuedAt(),
                claims.getExpiration(),
                isTokenExpired(token)
            );
        } catch (Exception e) {
            return "Invalid token: " + e.getMessage();
        }
    }

}
