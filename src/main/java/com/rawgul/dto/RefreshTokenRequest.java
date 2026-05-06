package com.rawgul.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token Request DTO
 * 
 * Request object for refreshing access tokens using a valid refresh token.
 * 
 * Endpoint: POST /api/auth/refresh
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * Refresh token obtained during login.
     * Must be a valid, non-expired refresh token.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

}
