package com.rawgul.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for user registration from the frontend sign-up.html form
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @Size(max = 50, message = "Full name must not exceed 50 characters")
    private String fullname;

    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Mobile number must be valid")
    private String mobile;

    private Set<String> roles;

    // Helper method to split full name
    public void parseFullName() {
        if (fullname != null && !fullname.trim().isEmpty()) {
            String[] nameParts = fullname.trim().split("\\s+", 2);
            if (nameParts.length > 0) {
                this.firstName = nameParts[0];
            }
            if (nameParts.length > 1) {
                this.lastName = nameParts[1];
            }
        }
    }

    // Validation helper
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

}
