# Spring Security Configuration Documentation

Comprehensive guide to the Spring Security setup for Finance Tracker application.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Security Architecture](#security-architecture)
- [Components](#components)
- [Configuration](#configuration)
- [Authentication Flow](#authentication-flow)
- [Authorization](#authorization)
- [CORS Configuration](#cors-configuration)
- [Testing](#testing)
- [Production Considerations](#production-considerations)

---

## 🔒 Overview

The Finance Tracker application uses **JWT (JSON Web Token)** based authentication with Spring Security 6.x. This provides:

✅ **Stateless Authentication** - No server-side sessions  
✅ **Scalability** - Easy horizontal scaling  
✅ **Cross-Origin Support** - CORS configured for frontend integration  
✅ **Role-Based Access Control** - Fine-grained permissions with @PreAuthorize  
✅ **BCrypt Password Encryption** - Industry-standard password hashing  
✅ **Public Endpoints** - Open access for registration, password reset, support tickets

---

## 🏗️ Security Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Request                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    CORS Filter (Step 1)                          │
│  - Validates origin                                              │
│  - Adds CORS headers                                             │
│  - Handles preflight requests                                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter (Step 2)                    │
│  1. Extract JWT from Authorization header                        │
│  2. Validate token signature and expiration                      │
│  3. Extract username from token                                  │
│  4. Load user details from database                              │
│  5. Set authentication in SecurityContext                        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain (Step 3)               │
│  - Check if endpoint is public or protected                      │
│  - If protected: Check if authenticated                          │
│  - If not authenticated: Return 401 via AuthEntryPointJwt        │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              Method Security (Step 4)                            │
│  - Check @PreAuthorize("hasRole('ADMIN')")                       │
│  - If authorized: Proceed to controller                          │
│  - If not authorized: Return 403 Forbidden                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Method                             │
│  - Business logic execution                                      │
│  - Return response                                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧩 Components

### 1. SecurityConfig.java

**Location:** `src/main/java/com/rawgul/config/SecurityConfig.java`

Main Spring Security configuration class.

**Key Responsibilities:**
- Define public vs protected endpoints
- Configure CORS for frontend integration
- Set up JWT authentication filter
- Configure password encoder (BCrypt)
- Define authentication provider
- Disable CSRF (stateless JWT approach)
- Configure session management (stateless)

**Key Beans:**

#### `passwordEncoder()`
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // 2^12 = 4096 rounds
}
```
- Uses BCrypt algorithm with strength 12 (configurable)
- Higher strength = more secure but slower
- Recommended range: 10-14 for production

#### `corsConfigurationSource()`
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    // Allows frontend origins to access API
    // Configurable via application.properties
}
```
- Allows specified origins (localhost:3000, localhost:8080, etc.)
- Permits common HTTP methods (GET, POST, PUT, DELETE)
- Exposes Authorization header for JWT
- Caches preflight responses for 1 hour

#### `filterChain(HttpSecurity http)`
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    // Main security configuration
}
```

**Public Endpoints (No Authentication Required):**

| Endpoint Pattern | Description |
|-----------------|-------------|
| `/api/auth/**` | All authentication endpoints (login, register, signup) |
| `/api/password-reset/**` | Password reset workflow (request, validate, reset) |
| `/api/support/tickets` | POST - Create support ticket (public) |
| `/api/support/ticket/{ticketNumber}` | GET - Check ticket status by number |
| `/*.html` | All HTML pages (login, signup, support, etc.) |
| `/*.css` | All CSS files |
| `/*.js` | All JavaScript files |
| `/assets/**` | Static assets (images, fonts) |
| `/favicon_io/**` | Favicon files |
| `/actuator/**` | Health checks and metrics |

**Protected Endpoints (Authentication Required):**

| Endpoint Pattern | Required Role | Description |
|-----------------|---------------|-------------|
| `/api/users/**` | USER or ADMIN | User profile and management |
| `/api/support/tickets` | USER or ADMIN | GET - View user's tickets |
| `/api/support/tickets/{id}` | USER or ADMIN | GET/PUT - View/Update ticket |
| All other `/api/**` | USER | All other API endpoints |

---

### 2. JwtAuthenticationFilter.java

**Location:** `src/main/java/com/rawgul/security/JwtAuthenticationFilter.java`

Intercepts every request to validate JWT tokens.

**Responsibilities:**
1. Extract JWT from `Authorization: Bearer <token>` header
2. Validate token signature and expiration
3. Extract username from token claims
4. Load user details from database
5. Set authentication in Spring Security context

**Key Features:**
- Extends `OncePerRequestFilter` (runs once per request)
- Skips static resources (.css, .js, images)
- Graceful error handling (logs but continues)
- Extracts request details for audit logging

**Flow:**
```java
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
           │
           ▼
parseJwt(request) → Extract token (remove "Bearer " prefix)
           │
           ▼
jwtTokenProvider.validateToken(jwt) → Validate signature & expiration
           │
           ▼
jwtTokenProvider.getUsernameFromToken(jwt) → Extract username
           │
           ▼
userDetailsService.loadUserByUsername(username) → Load from database
           │
           ▼
Create UsernamePasswordAuthenticationToken
           │
           ▼
SecurityContextHolder.getContext().setAuthentication(auth)
```

**Static Resource Optimization:**
```java
@Override
protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.endsWith(".css") || path.endsWith(".js") || ...;
}
```
- Skips JWT validation for static files
- Improves performance
- Reduces unnecessary database queries

---

### 3. AuthEntryPointJwt.java

**Location:** `src/main/java/com/rawgul/security/AuthEntryPointJwt.java`

Handles authentication errors (HTTP 401 Unauthorized).

**Triggered When:**
- No JWT token provided
- Invalid JWT token (expired, malformed, wrong signature)
- User not found or account disabled

**Response Format:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required. Please log in to access this resource.",
  "path": "/api/users/profile",
  "timestamp": "2025-11-03T10:30:00",
  "hint": "Please provide a valid JWT token in the Authorization header (Bearer <token>)"
}
```

**Features:**
- User-friendly error messages (converts technical errors)
- Logs authentication failures with IP address
- Extracts client IP from various proxy headers
- Includes helpful hints for common errors

**Error Message Mapping:**

| Technical Message | User-Friendly Message |
|------------------|----------------------|
| "Full authentication is required" | "Authentication required. Please log in to access this resource." |
| "JWT expired" | "Your session has expired. Please log in again." |
| "JWT signature" | "Invalid authentication token. Please log in again." |
| "User is disabled" | "Your account has been disabled. Please contact support." |
| "User account is locked" | "Your account has been locked. Please contact support or reset your password." |

---

### 4. UserDetailsServiceImpl.java

**Location:** `src/main/java/com/rawgul/security/UserDetailsServiceImpl.java`

Loads user details from database for authentication.

**Key Method:**
```java
@Override
public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    return UserDetailsImpl.build(user);
}
```

**Returns:** `UserDetails` object with:
- Username
- Password (encrypted)
- Authorities (roles)
- Account status (enabled, locked, expired)

---

### 5. UserDetailsImpl.java

**Location:** `src/main/java/com/rawgul/security/UserDetailsImpl.java`

Spring Security's `UserDetails` implementation.

**Fields:**
- `id` - User ID
- `username` - Username
- `email` - Email address
- `password` - Encrypted password
- `authorities` - Granted authorities (roles)
- `enabled` - Account active status
- `accountNonLocked` - Account lock status

---

## ⚙️ Configuration

### application.properties

```properties
# Security Configuration
app.security.bcrypt-strength=12

# CORS Configuration
app.cors.allowed-origins=http://localhost:3000,http://localhost:8080,http://127.0.0.1:5500

# JWT Configuration
jwt.secret=your_jwt_secret_key_change_this_in_production_minimum_256_bits
jwt.expiration=86400000

# Logging
logging.level.org.springframework.security=DEBUG
```

### Configuration Properties Explained

| Property | Default | Description |
|----------|---------|-------------|
| `app.security.bcrypt-strength` | 12 | BCrypt rounds (2^n), range: 4-31 |
| `app.cors.allowed-origins` | localhost URLs | Comma-separated allowed origins |
| `jwt.secret` | (none) | HMAC secret key (min 256 bits) |
| `jwt.expiration` | 86400000 | Token lifetime in milliseconds (24h) |

---

## 🔐 Authentication Flow

### 1. User Registration

```
POST /api/auth/register
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123!"
}
           │
           ▼
AuthController.registerUser()
           │
           ▼
AuthService.registerUser()
           │
           ▼
Check username/email uniqueness
           │
           ▼
Validate password strength
           │
           ▼
Hash password with BCrypt (12 rounds)
           │
           ▼
Save user to database
           │
           ▼
Return success message
```

**Password is hashed before storage:**
```java
String encodedPassword = passwordEncoder.encode(rawPassword);
// Raw: "Password123!"
// Hashed: "$2a$12$xYz...ABC" (60 characters)
```

---

### 2. User Login

```
POST /api/auth/login
{
  "username": "johndoe",
  "password": "Password123!"
}
           │
           ▼
AuthController.authenticateUser()
           │
           ▼
AuthenticationManager.authenticate()
           │
           ▼
UserDetailsServiceImpl.loadUserByUsername()
           │
           ▼
Load user from database
           │
           ▼
BCrypt.matches(rawPassword, hashedPassword)
           │
           ▼
Generate JWT token
           │
           ▼
Return token in response
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

---

### 3. Accessing Protected Endpoint

```
GET /api/users/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
           │
           ▼
JwtAuthenticationFilter.doFilterInternal()
           │
           ▼
Extract JWT from Authorization header
           │
           ▼
Validate JWT signature and expiration
           │
           ▼
Extract username from JWT claims
           │
           ▼
Load user details from database
           │
           ▼
Set authentication in SecurityContext
           │
           ▼
SecurityFilterChain checks if endpoint is protected
           │
           ▼
Authentication exists → Allow access
           │
           ▼
UserController.getCurrentUserProfile()
           │
           ▼
Return user profile data
```

---

## 🛡️ Authorization

### Role-Based Access Control

The application uses Spring Security's method-level security with `@PreAuthorize` annotations.

**Available Roles:**
- `ROLE_USER` - Regular user
- `ROLE_ADMIN` - Administrator

### Example Usage

```java
// Admin-only endpoint
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<User>> getAllUsers() {
    // Only accessible by ADMIN role
}

// User or Admin can access
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    // Accessible by USER or ADMIN role
}

// Any authenticated user
@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<User> getCurrentUserProfile() {
    // Accessible by any authenticated user
}

// Public endpoint (no annotation needed)
@PostMapping("/tickets")
public ResponseEntity<SupportTicket> createTicket(@Valid @RequestBody SupportTicketRequest request) {
    // Publicly accessible, no authentication required
}
```

### Common @PreAuthorize Expressions

| Expression | Description |
|-----------|-------------|
| `isAuthenticated()` | User is logged in |
| `hasRole('ADMIN')` | User has ADMIN role |
| `hasAnyRole('USER', 'ADMIN')` | User has any of the listed roles |
| `hasAuthority('DELETE_USER')` | User has specific authority |
| `permitAll()` | Allow everyone (including anonymous) |
| `denyAll()` | Deny everyone |
| `principal.username == #username` | Current user matches parameter |

---

## 🌐 CORS Configuration

### Allowed Origins

Configured in `application.properties`:
```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:8080,http://127.0.0.1:5500
```

**Development:**
- `http://localhost:3000` - React/Next.js dev server
- `http://localhost:8080` - Spring Boot (same-origin)
- `http://127.0.0.1:5500` - Live Server (VS Code)

**Production:**
```properties
app.cors.allowed-origins=https://yourdomain.com,https://www.yourdomain.com
```

### Allowed Methods

```java
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
));
```

### Allowed Headers

```java
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization",      // JWT token
    "Content-Type",       // JSON requests
    "Accept",             // Response format
    "X-Requested-With",   // AJAX indicator
    "Cache-Control"       // Caching directives
));
```

### Exposed Headers

```java
configuration.setExposedHeaders(Arrays.asList(
    "Authorization",        // JWT token in response
    "Content-Disposition"   // File downloads
));
```

### Preflight Caching

```java
configuration.setMaxAge(3600L); // 1 hour
```

Browsers cache preflight (OPTIONS) requests for 1 hour to reduce network overhead.

---

## 🧪 Testing

### Testing Authentication

#### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "confirmPassword": "Test123!",
    "fullname": "Test User"
  }'
```

**Expected Response:**
```json
{
  "message": "User registered successfully with ID: 1"
}
```

#### 2. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test123!"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTczMDYyODAwMCwiZXhwIjoxNzMwNzE0NDAwfQ.xyz",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

#### 3. Access Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Expected Response:**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "firstName": null,
  "lastName": null,
  "active": true,
  "createdDate": "2025-11-03T10:30:00"
}
```

#### 4. Access Without Token (Should Fail)

```bash
curl -X GET http://localhost:8080/api/users/profile
```

**Expected Response:** `401 Unauthorized`
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required. Please log in to access this resource.",
  "path": "/api/users/profile",
  "timestamp": "2025-11-03T10:30:00",
  "hint": "Please provide a valid JWT token in the Authorization header (Bearer <token>)"
}
```

### Testing CORS

#### From Frontend JavaScript

```javascript
// Login
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'testuser',
    password: 'Test123!'
  })
})
.then(response => response.json())
.then(data => {
  // Save token
  localStorage.setItem('jwt', data.token);
  console.log('Login successful');
})
.catch(error => console.error('Error:', error));

// Access protected endpoint
const token = localStorage.getItem('jwt');

fetch('http://localhost:8080/api/users/profile', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log('Profile:', data))
.catch(error => console.error('Error:', error));
```

---

## 🚀 Production Considerations

### 1. JWT Secret Key

**Development:**
```properties
jwt.secret=your_jwt_secret_key_change_this_in_production_minimum_256_bits
```

**Production:**
```properties
# Use environment variable
jwt.secret=${JWT_SECRET}
```

Generate a strong secret:
```bash
# Generate 256-bit random key (Base64 encoded)
openssl rand -base64 32
```

**Important:** Never commit production secrets to Git!

---

### 2. CORS Origins

**Development:**
```properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:8080
```

**Production:**
```properties
# Only allow your production domain
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:https://yourdomain.com}
```

---

### 3. BCrypt Strength

**Development:** 12 (fast enough for development)
**Production:** 12-14 (balance security and performance)

```properties
# Lower = faster but less secure
# Higher = slower but more secure
app.security.bcrypt-strength=${BCRYPT_STRENGTH:12}
```

**Performance Impact:**
| Strength | Rounds | Time (approx) |
|----------|--------|---------------|
| 10 | 1,024 | ~70ms |
| 12 | 4,096 | ~250ms |
| 14 | 16,384 | ~1s |
| 16 | 65,536 | ~4s |

---

### 4. HTTPS

Always use HTTPS in production:

```properties
# Force HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

---

### 5. Token Expiration

**Development:** 24 hours
```properties
jwt.expiration=86400000
```

**Production:** Consider shorter expiration with refresh tokens
```properties
# 1 hour
jwt.expiration=3600000
```

---

### 6. Security Headers

Add security headers in production:

```java
http.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'")
    .frameOptions().deny()
    .xssProtection().and()
    .contentTypeOptions().and()
    .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
);
```

---

### 7. Rate Limiting

Consider adding rate limiting for authentication endpoints:

```java
// Spring Cloud Gateway or bucket4j
@RateLimiter(name = "authApi")
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // ...
}
```

---

### 8. Actuator Security

Restrict actuator endpoints in production:

```properties
# Only expose health endpoint publicly
management.endpoints.web.exposure.include=health

# Require authentication for other endpoints
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=when-authorized
```

---

### 9. Logging

**Development:**
```properties
logging.level.org.springframework.security=DEBUG
```

**Production:**
```properties
# Reduce noise, only log warnings/errors
logging.level.org.springframework.security=WARN
logging.level.com.rawgul.security=INFO
```

---

### 10. Database Credentials

**Never hardcode in production:**

```properties
# Use environment variables
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

---

## 📚 Additional Resources

### Spring Security Documentation
- [Official Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT Guide](https://jwt.io/introduction)
- [BCrypt Algorithm](https://en.wikipedia.org/wiki/Bcrypt)

### Related Files
- `SecurityConfig.java` - Main security configuration
- `JwtAuthenticationFilter.java` - JWT validation filter
- `AuthEntryPointJwt.java` - Authentication error handler
- `JwtTokenProvider.java` - JWT token generation/validation
- `UserDetailsServiceImpl.java` - User loading service
- `REST_API_DOCUMENTATION.md` - API endpoint documentation
- `SERVICE_LAYER_DOCUMENTATION.md` - Business logic documentation

---

**Last Updated**: November 3, 2025  
**Version**: 1.0  
**Author**: Raghul
