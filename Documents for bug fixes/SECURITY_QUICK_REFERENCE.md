# Spring Security Quick Reference

Fast reference guide for developers working with the Finance Tracker security setup.

---

## 🚀 Quick Start

### 1. Testing Authentication (cURL)

```bash
# Register new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test123!","confirmPassword":"Test123!","fullname":"Test User"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}'

# Save the token from response, then:
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Access protected endpoint
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🔐 Public vs Protected Endpoints

### ✅ Public (No Auth Required)

```java
// Authentication
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/signup          // Legacy

// Password Reset
POST   /api/password-reset/request
GET    /api/password-reset/validate?token=xyz
POST   /api/password-reset/reset

// Support (Public)
POST   /api/support/tickets       // Create ticket
GET    /api/support/ticket/{ticketNumber}

// Static Files
GET    /*.html
GET    /*.css
GET    /*.js
GET    /assets/**
```

### 🔒 Protected (Auth Required)

```java
// User Management
GET    /api/users                 // ADMIN only
GET    /api/users/{id}            // USER/ADMIN
GET    /api/users/profile         // Own profile
PUT    /api/users/profile         // Update profile
POST   /api/users/change-password // Change password
PUT    /api/users/{id}/activate   // ADMIN only
PUT    /api/users/{id}/deactivate // ADMIN only
DELETE /api/users/{id}            // ADMIN only

// Support Tickets (Authenticated)
GET    /api/support/tickets       // User's tickets
GET    /api/support/tickets/{id}
PUT    /api/support/tickets/{id}  // ADMIN only
```

---

## 🛡️ Adding Security to Controllers

### Public Endpoint (No Security)

```java
@PostMapping("/api/auth/register")
public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest request) {
    // No annotation needed - configured in SecurityConfig
}
```

### Any Authenticated User

```java
@GetMapping("/profile")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<User> getProfile() {
    UserDetails userDetails = (UserDetails) SecurityContextHolder
        .getContext().getAuthentication().getPrincipal();
    // ...
}
```

### Admin Only

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<List<User>> getAllUsers() {
    // Only ADMIN can access
}
```

### User or Admin

```java
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public ResponseEntity<User> getUserById(@PathVariable Long id) {
    // USER or ADMIN can access
}
```

### Custom Logic

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN') or @userService.isOwner(#id, principal.username)")
public ResponseEntity<?> deleteUser(@PathVariable Long id) {
    // ADMIN or resource owner can delete
}
```

---

## 👤 Getting Current User

### In Controller

```java
@GetMapping("/profile")
public ResponseEntity<User> getCurrentUser() {
    // Method 1: From SecurityContext
    UserDetails userDetails = (UserDetails) SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();
    
    String username = userDetails.getUsername();
    
    // Method 2: Inject directly
    User user = userService.getUserByUsername(username);
    return ResponseEntity.ok(user);
}

// Or with @AuthenticationPrincipal
@GetMapping("/profile")
public ResponseEntity<User> getCurrentUser(
    @AuthenticationPrincipal UserDetailsImpl userDetails
) {
    Long userId = userDetails.getId();
    User user = userService.getUserById(userId);
    return ResponseEntity.ok(user);
}
```

### In Service Layer

```java
@Service
public class UserService {
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user");
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        return getUserById(userDetails.getId());
    }
}
```

---

## 🔑 Password Encoding

### Encoding (Registration/Password Change)

```java
@Service
public class UserService {
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void createUser(String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }
}
```

### Validation (Login)

```java
// Handled automatically by Spring Security
// No manual validation needed

// Manual validation (if needed):
if (passwordEncoder.matches(rawPassword, encodedPassword)) {
    // Password correct
}
```

---

## 🌐 Frontend Integration

### JavaScript/Fetch API

```javascript
// Login
async function login(username, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  
  const data = await response.json();
  
  if (response.ok) {
    // Save token
    localStorage.setItem('jwt', data.token);
    localStorage.setItem('user', JSON.stringify(data));
    return data;
  } else {
    throw new Error(data.message);
  }
}

// Get Profile (with JWT)
async function getProfile() {
  const token = localStorage.getItem('jwt');
  
  const response = await fetch('http://localhost:8080/api/users/profile', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 401) {
    // Token expired, redirect to login
    window.location.href = '/login.html';
  }
}

// Logout
function logout() {
  localStorage.removeItem('jwt');
  localStorage.removeItem('user');
  window.location.href = '/login.html';
}
```

### React/Axios

```javascript
import axios from 'axios';

// Configure axios instance
const api = axios.create({
  baseURL: 'http://localhost:8080/api'
});

// Add JWT to all requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('jwt');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 errors
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Usage
const login = async (username, password) => {
  const response = await api.post('/auth/login', { username, password });
  localStorage.setItem('jwt', response.data.token);
  return response.data;
};

const getProfile = () => api.get('/users/profile');
```

---

## 🧪 Testing Tips

### Test Public Endpoint

```bash
# Should work without token
curl http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}'
```

### Test Protected Endpoint

```bash
# Should return 401
curl http://localhost:8080/api/users/profile

# Should work with token
curl http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

### Test CORS

```bash
# Preflight request
curl -X OPTIONS http://localhost:8080/api/users/profile \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization"

# Should return CORS headers
```

### Test Admin Endpoint

```bash
# With USER role - should return 403
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer $USER_TOKEN"

# With ADMIN role - should work
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 🐛 Common Issues

### Issue: 401 Unauthorized

**Causes:**
- No token provided
- Token expired
- Invalid token format
- User not found

**Solution:**
```bash
# Check token format
echo $TOKEN | cut -d'.' -f2 | base64 -d

# Login again to get fresh token
curl -X POST http://localhost:8080/api/auth/login ...
```

### Issue: 403 Forbidden

**Causes:**
- User authenticated but lacks required role
- @PreAuthorize check failed

**Solution:**
```java
// Check user roles in database
SELECT u.username, r.name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id;

// Assign ADMIN role if needed
INSERT INTO user_roles (user_id, role_id) 
VALUES (1, (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'));
```

### Issue: CORS Error

**Error in browser:**
```
Access to fetch at 'http://localhost:8080/api/users/profile' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solution:**
```properties
# Add your frontend origin to application.properties
app.cors.allowed-origins=http://localhost:3000,http://localhost:8080
```

### Issue: Password Doesn't Match

**Cause:** Password not encoded before saving

**Solution:**
```java
// Always encode before saving
String encodedPassword = passwordEncoder.encode(rawPassword);
user.setPassword(encodedPassword);
```

---

## 📋 Configuration Checklist

### Development

- [x] `jwt.secret` set (any value)
- [x] `jwt.expiration` = 86400000 (24 hours)
- [x] `app.cors.allowed-origins` includes frontend URL
- [x] `logging.level.org.springframework.security=DEBUG`
- [x] Public endpoints configured in SecurityConfig
- [x] Roles created in database

### Production

- [ ] `jwt.secret` from environment variable (256+ bits)
- [ ] `jwt.expiration` reduced (3600000 = 1 hour)
- [ ] `app.cors.allowed-origins` only production domain
- [ ] `logging.level.org.springframework.security=WARN`
- [ ] HTTPS enabled
- [ ] Rate limiting configured
- [ ] Actuator endpoints secured
- [ ] Database credentials from env variables

---

## 🔗 Related Documentation

- [SPRING_SECURITY_CONFIGURATION.md](SPRING_SECURITY_CONFIGURATION.md) - Full documentation
- [REST_API_DOCUMENTATION.md](REST_API_DOCUMENTATION.md) - API endpoints
- [SERVICE_LAYER_DOCUMENTATION.md](SERVICE_LAYER_DOCUMENTATION.md) - Business logic

---

**Quick Reference Version**: 1.0  
**Last Updated**: November 3, 2025
