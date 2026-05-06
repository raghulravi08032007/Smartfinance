package com.rawgul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JWT Response DTO
 * 
 * Response object returned after successful authentication.
 * Contains both access token and refresh token for complete token management.
 * 
 * Fields:
 * - accessToken: Short-lived token for API authentication (24 hours default)
 * - refreshToken: Long-lived token for token renewal (7 days default)
 * - tokenType: Always "Bearer" for Authorization header format
 * - id: User's database ID
 * - username: User's username
 * - email: User's email address
 * - roles: User's granted authorities (e.g., ["ROLE_USER", "ROLE_ADMIN"])
 * 
 * @author Raghul
 * @version 2.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    /**
     * Access token for API authentication.
     * Include in Authorization header: Authorization: Bearer <accessToken>
     * Short-lived (default: 24 hours)
     */
    @JsonProperty("accessToken")
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens.
     * Use when access token expires to get a new one without re-authentication.
     * Long-lived (default: 7 days)
     */
    @JsonProperty("refreshToken")
    private String refreshToken;

    /**
     * Token type for Authorization header.
     * Always "Bearer"
     */
    @JsonProperty("tokenType")
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * User's database ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * User's username
     */
    @JsonProperty("username")
    private String username;

    /**
     * User's email address
     */
    @JsonProperty("email")
    private String email;

    /**
     * User's granted authorities/roles
     * Example: ["ROLE_USER", "ROLE_ADMIN"]
     */
    @JsonProperty("roles")
    private List<String> roles;

    /**
     * Legacy constructor for backward compatibility (access token only).
     * 
     * @deprecated Use constructor with refreshToken or builder pattern
     */
    @Deprecated
    public JwtResponse(String token, Long id, String username, String email, List<String> roles) {
        this.accessToken = token;
        this.refreshToken = null;
        this.tokenType = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    /**
     * Constructor with both access and refresh tokens.
     * 
     * @param accessToken Short-lived access token
     * @param refreshToken Long-lived refresh token
     * @param id User's database ID
     * @param username User's username
     * @param email User's email
     * @param roles User's roles
     */
    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    /**
     * Getter for access token (backwards compatibility).
     * Maps to "token" field in JSON for legacy clients.
     */
    @JsonProperty("token")
    public String getToken() {
        return accessToken;
    }

    /**
     * Setter for access token (backwards compatibility).
     */
    public void setToken(String token) {
        this.accessToken = token;
    }

}
