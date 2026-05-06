# JWT Utility Documentation

Comprehensive guide to the JwtTokenProvider utility class for JWT token operations.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Configuration](#configuration)
- [Token Types](#token-types)
- [Core Operations](#core-operations)
- [Usage Examples](#usage-examples)
- [Token Structure](#token-structure)
- [Security Considerations](#security-considerations)
- [Testing](#testing)

---

## 🔍 Overview

**Class:** `JwtTokenProvider`  
**Location:** `src/main/java/com/rawgul/security/JwtTokenProvider.java`  
**Version:** 2.0

The `JwtTokenProvider` is a comprehensive utility class for managing JSON Web Tokens (JWT) in the Finance Tracker application. It handles token generation, validation, and claims extraction for both **access tokens** and **refresh tokens**.

---

## ✨ Features

### Token Generation
- ✅ **Access Token Generation** - Short-lived tokens for API authentication
- ✅ **Refresh Token Generation** - Long-lived tokens for token renewal
- ✅ **Custom Claims** - User ID, roles, token type included in payload
- ✅ **Configurable Expiration** - Via application.properties

### Token Validation
- ✅ **Signature Verification** - HMAC-SHA256 validation
- ✅ **Expiration Check** - Ensures tokens are not expired
- ✅ **Token Type Validation** - Distinguishes access vs refresh tokens
- ✅ **Comprehensive Error Handling** - Detailed logging for debugging

### Claims Extraction
- ✅ **Username Extraction** - Get user's username from token
- ✅ **User ID Extraction** - Get user's database ID
- ✅ **Roles Extraction** - Get user's granted authorities
- ✅ **Expiration Date** - Get token expiration timestamp
- ✅ **Issued Date** - Get token creation timestamp

### Advanced Operations
- ✅ **Token Expiry Check** - Check if token is expired
- ✅ **Remaining Time** - Calculate time until expiration
- ✅ **Token Refresh** - Generate new access token from refresh token
- ✅ **Token Info** - Get human-readable token summary

---

## ⚙️ Configuration

### application.properties

```properties
# JWT Secret Key (HMAC-SHA256)
# IMPORTANT: Change in production! Minimum 256 bits (32 characters)
jwt.secret=your_jwt_secret_key_change_this_in_production_minimum_256_bits_for_security

# Access Token Expiration (milliseconds)
# Default: 86400000 ms = 24 hours
jwt.expiration=86400000

# Refresh Token Expiration (milliseconds)
# Default: 604800000 ms = 7 days
jwt.refresh.expiration=604800000
```

### Configuration Values

| Property | Default | Description |
|----------|---------|-------------|
| `jwt.secret` | (required) | HMAC secret key, min 256 bits |
| `jwt.expiration` | 86400000 | Access token lifetime (24 hours) |
| `jwt.refresh.expiration` | 604800000 | Refresh token lifetime (7 days) |

### Environment-Specific Configuration

**Development:**
```properties
jwt.secret=dev_secret_key_minimum_256_bits_for_hmac_sha256_algorithm
jwt.expiration=86400000        # 24 hours
jwt.refresh.expiration=604800000  # 7 days
```

**Production:**
```properties
# Use environment variables
jwt.secret=${JWT_SECRET}
jwt.expiration=3600000         # 1 hour (more secure)
jwt.refresh.expiration=2592000000  # 30 days
```

**Generate Secure Secret:**
```bash
# Generate 256-bit random secret (Base64 encoded)
openssl rand -base64 32

# Output example:
# XkZ2pQvN8R5fW7mL9tYcH3jK6sA1bD4eF0gU2iO5xP8=
```

---

## 🎫 Token Types

### Access Token

**Purpose:** Short-lived token for API authentication  
**Lifetime:** 24 hours (default)  
**Use Case:** Include in Authorization header for every API request

**Claims:**
- `sub` (subject) - Username
- `userId` - User's database ID
- `roles` - User's granted authorities (e.g., ["ROLE_USER", "ROLE_ADMIN"])
- `token_type` - "access"
- `iat` (issued at) - Token creation timestamp
- `exp` (expiration) - Token expiration timestamp

**Example Usage:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

### Refresh Token

**Purpose:** Long-lived token for obtaining new access tokens  
**Lifetime:** 7 days (default)  
**Use Case:** Renew access token without requiring re-authentication

**Claims:**
- `sub` (subject) - Username
- `userId` - User's database ID
- `token_type` - "refresh"
- `iat` (issued at) - Token creation timestamp
- `exp` (expiration) - Token expiration timestamp

**Note:** Refresh tokens do NOT include roles (must be fetched fresh from database)

---

## 🔧 Core Operations

### 1. Token Generation

#### Generate Access Token from Authentication

```java
@Autowired
private JwtTokenProvider jwtTokenProvider;

// After successful authentication
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);

String accessToken = jwtTokenProvider.generateToken(authentication);
```

#### Generate Access Token from Username

```java
String username = "johndoe";
Long userId = 1L;
List<String> roles = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

String accessToken = jwtTokenProvider.generateTokenFromUsername(username, userId, roles);
```

#### Generate Refresh Token

```java
// From Authentication
String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

// From username and userId
String refreshToken = jwtTokenProvider.generateRefreshTokenFromUsername(username, userId);
```

---

### 2. Token Validation

#### Validate Any Token

```java
String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

if (jwtTokenProvider.validateToken(token)) {
    System.out.println("Token is valid");
} else {
    System.out.println("Token is invalid");
}
```

#### Validate Refresh Token Specifically

```java
if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
    System.out.println("Refresh token is valid");
}
```

#### Check Token Expiration

```java
if (jwtTokenProvider.isTokenExpired(token)) {
    System.out.println("Token has expired");
}
```

#### Get Remaining Time

```java
long remainingMs = jwtTokenProvider.getTokenRemainingTime(token);
long remainingMinutes = remainingMs / 1000 / 60;

System.out.println("Token expires in " + remainingMinutes + " minutes");
```

---

### 3. Claims Extraction

#### Extract Username

```java
String username = jwtTokenProvider.getUsernameFromToken(token);
System.out.println("Username: " + username);
```

#### Extract User ID

```java
Long userId = jwtTokenProvider.getUserIdFromToken(token);
System.out.println("User ID: " + userId);
```

#### Extract Roles

```java
List<String> roles = jwtTokenProvider.getRolesFromToken(token);
System.out.println("Roles: " + roles);
// Output: Roles: [ROLE_USER, ROLE_ADMIN]
```

#### Extract Expiration Date

```java
Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
System.out.println("Expires: " + expirationDate);
```

#### Extract Issued Date

```java
Date issuedAt = jwtTokenProvider.getIssuedAtDateFromToken(token);
System.out.println("Issued: " + issuedAt);
```

---

### 4. Token Type Checks

#### Check if Access Token

```java
if (jwtTokenProvider.isAccessToken(token)) {
    System.out.println("This is an access token");
}
```

#### Check if Refresh Token

```java
if (jwtTokenProvider.isRefreshToken(token)) {
    System.out.println("This is a refresh token");
}
```

---

### 5. Token Refresh

#### Refresh Access Token

```java
String refreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

// Get user's current roles from database
User user = userService.getUserByUsername(
    jwtTokenProvider.getUsernameFromToken(refreshToken)
);
List<String> currentRoles = user.getRoles().stream()
    .map(role -> role.getName())
    .collect(Collectors.toList());

// Generate new access token
String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken, currentRoles);
```

**Why pass roles?** Roles may have changed since refresh token was issued. We always use current roles from database.

---

### 6. Token Info (Debugging)

```java
String info = jwtTokenProvider.getTokenInfo(token);
System.out.println(info);

// Output:
// Token Info - Type: access, Subject: johndoe, 
// Issued: Sun Nov 03 10:30:00 UTC 2025, 
// Expires: Mon Nov 04 10:30:00 UTC 2025, 
// Expired: false
```

---

## 💡 Usage Examples

### Complete Login Flow with Tokens

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);
        
        // Get user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        
        // Return response
        return ResponseEntity.ok(new JwtResponse(
            accessToken,
            refreshToken,
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getEmail(),
            roles
        ));
    }
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

---

### Token Refresh Endpoint

```java
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();
    
    // Validate refresh token
    if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new MessageResponse("Invalid refresh token"));
    }
    
    // Extract username
    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
    
    // Load current user roles from database
    User user = userService.getUserByUsername(username);
    List<String> roles = user.getRoles().stream()
        .map(role -> role.getName())
        .collect(Collectors.toList());
    
    // Generate new access token
    String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken, roles);
    
    return ResponseEntity.ok(new JwtResponse(
        newAccessToken,
        refreshToken,  // Same refresh token
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        roles
    ));
}
```

---

### Validate Token in Service

```java
@Service
public class TokenService {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    public boolean isValidToken(String token) {
        // Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            return false;
        }
        
        // Check if expired
        if (jwtTokenProvider.isTokenExpired(token)) {
            return false;
        }
        
        // Ensure it's an access token (not refresh token)
        if (!jwtTokenProvider.isAccessToken(token)) {
            return false;
        }
        
        return true;
    }
}
```

---

### Extract User Info in Controller

```java
@GetMapping("/profile")
public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
    // Extract token from "Bearer <token>"
    String token = authHeader.substring(7);
    
    // Extract user information
    String username = jwtTokenProvider.getUsernameFromToken(token);
    Long userId = jwtTokenProvider.getUserIdFromToken(token);
    List<String> roles = jwtTokenProvider.getRolesFromToken(token);
    
    // Get full user details from database
    User user = userService.getUserById(userId);
    
    return ResponseEntity.ok(user);
}
```

---

### Check Token Expiration (Frontend)

```java
@GetMapping("/token/info")
public ResponseEntity<?> getTokenInfo(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.substring(7);
    
    // Get expiration info
    Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);
    long remainingTime = jwtTokenProvider.getTokenRemainingTime(token);
    boolean isExpired = jwtTokenProvider.isTokenExpired(token);
    
    Map<String, Object> info = new HashMap<>();
    info.put("expirationDate", expirationDate);
    info.put("remainingTimeMs", remainingTime);
    info.put("remainingTimeMinutes", remainingTime / 1000 / 60);
    info.put("isExpired", isExpired);
    
    return ResponseEntity.ok(info);
}
```

**Response:**
```json
{
  "expirationDate": "2025-11-04T10:30:00",
  "remainingTimeMs": 43200000,
  "remainingTimeMinutes": 720,
  "isExpired": false
}
```

---

## 🔐 Token Structure

### Access Token Payload

```json
{
  "sub": "johndoe",
  "userId": 1,
  "roles": ["ROLE_USER", "ROLE_ADMIN"],
  "token_type": "access",
  "iat": 1730628000,
  "exp": 1730714400
}
```

**Decoded Token Structure:**

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "johndoe",           // Subject (username)
  "userId": 1,                // User database ID
  "roles": [                  // User authorities
    "ROLE_USER",
    "ROLE_ADMIN"
  ],
  "token_type": "access",     // Token type
  "iat": 1730628000,          // Issued at (Unix timestamp)
  "exp": 1730714400           // Expiration (Unix timestamp)
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

**Token Format:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiJqb2huZG9lIiwidXNlcklkIjoxLCJyb2xlcyI6WyJST0xFX1VTRVIiXX0.
xyz123abc456def789
│                                │                                  │
└─ Header (Base64)               └─ Payload (Base64)               └─ Signature
```

---

### Refresh Token Payload

```json
{
  "sub": "johndoe",
  "userId": 1,
  "token_type": "refresh",
  "iat": 1730628000,
  "exp": 1731232800
}
```

**Note:** Refresh tokens do NOT include roles. Roles must be fetched from database when refreshing.

---

## 🛡️ Security Considerations

### Secret Key

**Requirements:**
- Minimum 256 bits (32 characters) for HMAC-SHA256
- Use cryptographically random bytes
- Never commit to version control
- Use environment variables in production

**Generate Secure Key:**
```bash
# Option 1: OpenSSL
openssl rand -base64 32

# Option 2: Python
python -c "import secrets; print(secrets.token_urlsafe(32))"

# Option 3: Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

---

### Token Expiration

**Best Practices:**

| Environment | Access Token | Refresh Token |
|------------|--------------|---------------|
| Development | 24 hours | 7 days |
| Staging | 1-2 hours | 7-14 days |
| Production | 15-60 min | 30 days max |

**Recommendations:**
- Short access tokens = better security (limited window if stolen)
- Longer refresh tokens = better UX (less frequent re-auth)
- Implement token refresh before expiration
- Invalidate refresh tokens on logout

---

### Token Storage

**Frontend Storage:**

| Storage Method | Security | Persistence | Recommendation |
|---------------|----------|-------------|----------------|
| localStorage | ❌ Vulnerable to XSS | ✅ Persistent | ❌ Not recommended |
| sessionStorage | ❌ Vulnerable to XSS | ❌ Session only | ⚠️ Use with caution |
| HttpOnly Cookie | ✅ XSS protected | ✅ Configurable | ✅ **Recommended** |
| Memory only | ✅ Most secure | ❌ Lost on refresh | ⚠️ Good for access tokens |

**Best Practice:**
```javascript
// Store refresh token in HttpOnly cookie (backend sets it)
// Store access token in memory or sessionStorage
let accessToken = null;

function setAccessToken(token) {
    accessToken = token;
    // Don't store in localStorage!
}

function getAccessToken() {
    return accessToken;
}
```

---

### Token Validation

**Always Validate:**
1. ✅ Signature is valid (not tampered)
2. ✅ Token is not expired
3. ✅ Token type matches expected (access vs refresh)
4. ✅ User still exists in database
5. ✅ User account is active (not disabled/locked)
6. ✅ Roles haven't changed (for sensitive operations)

**Example:**
```java
public boolean validateTokenWithUserCheck(String token) {
    // Validate token structure and signature
    if (!jwtTokenProvider.validateToken(token)) {
        return false;
    }
    
    // Check user exists and is active
    String username = jwtTokenProvider.getUsernameFromToken(token);
    User user = userRepository.findByUsername(username)
        .orElse(null);
    
    if (user == null || !user.isActive() || user.isAccountLocked()) {
        return false;
    }
    
    return true;
}
```

---

## 🧪 Testing

### Unit Tests

```java
@SpringBootTest
public class JwtTokenProviderTest {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    public void testGenerateToken() {
        String username = "testuser";
        Long userId = 1L;
        List<String> roles = Arrays.asList("ROLE_USER");
        
        String token = jwtTokenProvider.generateTokenFromUsername(username, userId, roles);
        
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // Header.Payload.Signature
    }
    
    @Test
    public void testExtractUsername() {
        String username = "testuser";
        Long userId = 1L;
        List<String> roles = Arrays.asList("ROLE_USER");
        
        String token = jwtTokenProvider.generateTokenFromUsername(username, userId, roles);
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);
        
        assertEquals(username, extractedUsername);
    }
    
    @Test
    public void testValidateToken() {
        String username = "testuser";
        Long userId = 1L;
        List<String> roles = Arrays.asList("ROLE_USER");
        
        String token = jwtTokenProvider.generateTokenFromUsername(username, userId, roles);
        
        assertTrue(jwtTokenProvider.validateToken(token));
    }
    
    @Test
    public void testExpiredToken() throws InterruptedException {
        // This test requires short expiration time
        // Set jwt.expiration=1000 in test properties
        
        String token = jwtTokenProvider.generateTokenFromUsername("test", 1L, List.of("ROLE_USER"));
        
        Thread.sleep(2000); // Wait for expiration
        
        assertTrue(jwtTokenProvider.isTokenExpired(token));
    }
    
    @Test
    public void testRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshTokenFromUsername("test", 1L);
        
        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
    }
}
```

---

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
public class TokenIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Test
    public void testLoginAndAccessProtectedEndpoint() throws Exception {
        // Login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        
        // Extract token
        String response = loginResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(response);
        String token = json.getString("accessToken");
        
        // Access protected endpoint
        mockMvc.perform(get("/api/users/profile")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

---

## 📚 Related Documentation

- [SPRING_SECURITY_CONFIGURATION.md](SPRING_SECURITY_CONFIGURATION.md) - Security setup
- [REST_API_DOCUMENTATION.md](REST_API_DOCUMENTATION.md) - API endpoints
- [SECURITY_QUICK_REFERENCE.md](SECURITY_QUICK_REFERENCE.md) - Quick reference

---

## 🔗 Additional Resources

- [JWT.io](https://jwt.io/) - JWT debugger and documentation
- [JJWT Documentation](https://github.com/jwtk/jjwt) - Java JWT library
- [RFC 7519](https://tools.ietf.org/html/rfc7519) - JWT standard specification
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

---

**Last Updated**: November 3, 2025  
**Version**: 2.0  
**Author**: Raghul
