# REST API Documentation - Finance Tracker

Complete REST API reference for the Finance Tracker backend application.

---

## 📋 Table of Contents

- [Authentication](#authentication-api)
- [User Management](#user-management-api)
- [Support Tickets](#support-ticket-api)
- [Password Reset](#password-reset-api)
- [Request/Response Examples](#request-response-examples)

---

## 🔐 Authentication API

Base URL: `/api/auth`

### POST /api/auth/register
Register a new user account.

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "fullname": "John Doe",
  "mobile": "1234567890",
  "roles": ["user"]
}
```

**Response:** `201 CREATED`
```json
{
  "message": "User registered successfully with ID: 1"
}
```

**Validations:**
- Username: Required, unique, 3-20 characters
- Email: Required, unique, valid email format
- Password: Required, 8+ characters, must match confirmPassword
- Mobile: Optional, 10 digits
- Roles: Optional, defaults to ["user"]

---

### POST /api/auth/login
Authenticate user and get JWT token.

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "Password123!"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

**Error Responses:**
- `401 UNAUTHORIZED` - Invalid credentials
- `403 FORBIDDEN` - Account locked or inactive

---

### POST /api/auth/refresh
Refresh JWT token for authenticated user.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "roles": ["ROLE_USER"]
}
```

**Security:** Requires authentication

---

### POST /api/auth/logout
Logout user and clear security context.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "message": "Logged out successfully"
}
```

**Security:** Requires authentication

---

### GET /api/auth/me
Get current authenticated user details.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "mobileNumber": "1234567890",
  "active": true,
  "accountLocked": false,
  "roles": [
    {
      "id": 1,
      "name": "ROLE_USER"
    }
  ],
  "createdDate": "2025-11-03T10:30:00",
  "lastLogin": "2025-11-03T12:00:00"
}
```

**Security:** Requires authentication

---

## 👤 User Management API

Base URL: `/api/users`

### GET /api/users
Get all users (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "active": true
  }
]
```

**Security:** Requires ADMIN role

---

### GET /api/users/{id}
Get user by ID.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "mobileNumber": "1234567890",
  "active": true
}
```

**Security:** Requires USER or ADMIN role

---

### GET /api/users/profile
Get current user's profile.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "mobileNumber": "1234567890",
  "active": true,
  "createdDate": "2025-11-03T10:30:00"
}
```

**Security:** Requires authentication

---

### PUT /api/users/profile
Update current user's profile.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "mobileNumber": "9876543210"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "mobileNumber": "9876543210",
  "active": true
}
```

**Validations:**
- firstName: 2-50 characters
- lastName: 2-50 characters
- mobileNumber: 10 digits

**Security:** Requires authentication

---

### POST /api/users/change-password
Change current user's password.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Request Body:**
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!",
  "confirmPassword": "NewPassword456!"
}
```

**Response:** `200 OK`
```json
{
  "message": "Password changed successfully"
}
```

**Validations:**
- currentPassword: Must match existing password
- newPassword: 8+ chars, uppercase, lowercase, digit, special char
- confirmPassword: Must match newPassword

**Error Responses:**
- `400 BAD_REQUEST` - Current password incorrect or validation failed

**Security:** Requires authentication

---

### PUT /api/users/{id}/activate
Activate user account (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `200 OK`
```json
{
  "message": "User account activated successfully"
}
```

**Security:** Requires ADMIN role

---

### PUT /api/users/{id}/deactivate
Deactivate user account (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `200 OK`
```json
{
  "message": "User account deactivated successfully"
}
```

**Security:** Requires ADMIN role

---

### PUT /api/users/{id}/lock
Lock user account (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `200 OK`
```json
{
  "message": "User account locked successfully"
}
```

**Security:** Requires ADMIN role

---

### PUT /api/users/{id}/unlock
Unlock user account (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `200 OK`
```json
{
  "message": "User account unlocked successfully"
}
```

**Security:** Requires ADMIN role

---

### DELETE /api/users/{id}
Delete user account (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Response:** `204 NO CONTENT`

**Security:** Requires ADMIN role

---

## 🎫 Support Ticket API

Base URL: `/api/support`

### POST /api/support/tickets
Create a support ticket (public endpoint).

**Request Body:**
```json
{
  "fullname": "John Doe",
  "email": "john@example.com",
  "subject": "Unable to login",
  "query": "I forgot my password and cannot reset it",
  "priority": "high"
}
```

**Response:** `201 CREATED`
```json
{
  "id": 1,
  "ticketNumber": "TKT-1730628000000",
  "fullName": "John Doe",
  "contactEmail": "john@example.com",
  "subject": "Unable to login",
  "message": "I forgot my password and cannot reset it",
  "status": "OPEN",
  "priority": "HIGH",
  "createdDate": "2025-11-03T10:30:00"
}
```

**Validations:**
- fullname: Required
- email: Required, valid email format
- subject: Required
- query: Required (message)
- priority: Optional, values: low, medium, high, critical (defaults to medium)

---

### GET /api/support/tickets
Get support tickets.
- **Admin**: Get all tickets
- **User**: Get user's own tickets

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "ticketNumber": "TKT-1730628000000",
    "fullName": "John Doe",
    "contactEmail": "john@example.com",
    "subject": "Unable to login",
    "status": "OPEN",
    "priority": "HIGH",
    "assignedTo": null,
    "createdDate": "2025-11-03T10:30:00"
  }
]
```

**Security:** Requires USER or ADMIN role

---

### GET /api/support/tickets/{id}
Get ticket by ID.

**Headers:**
```
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "ticketNumber": "TKT-1730628000000",
  "fullName": "John Doe",
  "contactEmail": "john@example.com",
  "subject": "Unable to login",
  "message": "I forgot my password and cannot reset it",
  "status": "OPEN",
  "priority": "HIGH",
  "assignedTo": null,
  "resolutionNotes": null,
  "createdDate": "2025-11-03T10:30:00",
  "updatedDate": "2025-11-03T10:30:00"
}
```

**Security:** Requires USER or ADMIN role

---

### PUT /api/support/tickets/{id}
Update ticket (admin only).

**Headers:**
```
Authorization: Bearer <admin-jwt-token>
```

**Request Body:**
```json
{
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "assignedTo": "support-agent-1"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "ticketNumber": "TKT-1730628000000",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "assignedTo": "support-agent-1",
  "updatedDate": "2025-11-03T11:00:00"
}
```

**Status Values:**
- `OPEN` - Newly created ticket
- `IN_PROGRESS` - Being worked on
- `PENDING_USER` - Waiting for user response
- `RESOLVED` - Issue resolved
- `CLOSED` - Ticket closed

**Priority Values:**
- `LOW` - Low priority
- `MEDIUM` - Medium priority (default)
- `HIGH` - High priority
- `CRITICAL` - Critical issue

**Security:** Requires ADMIN role

---

### GET /api/support/ticket/{ticketNumber}
Get ticket by ticket number (public).

**Response:** `200 OK`
```json
{
  "id": 1,
  "ticketNumber": "TKT-1730628000000",
  "subject": "Unable to login",
  "status": "OPEN",
  "priority": "HIGH",
  "createdDate": "2025-11-03T10:30:00"
}
```

---

## 🔑 Password Reset API

Base URL: `/api/password-reset`

### POST /api/password-reset/request
Request password reset token.

**Request Body:**
```json
{
  "email": "john@example.com"
}
```

**Response:** `200 OK`
```json
{
  "message": "If an account exists with that email, a password reset link has been sent. Please check your email inbox."
}
```

**Business Logic:**
- Rate limiting: Max 3 requests per hour per user
- Token expires in 24 hours
- Email sent with reset link (TODO: Email service integration)
- Previous unused tokens are invalidated

**Note:** Returns same response regardless of email existence (security best practice)

---

### GET /api/password-reset/validate
Validate password reset token.

**Query Parameters:**
- `token`: The reset token

**Request:**
```
GET /api/password-reset/validate?token=abc123def456
```

**Response:** `200 OK`
```json
{
  "message": "Token is valid"
}
```

**Error Response:** `400 BAD_REQUEST`
```json
{
  "message": "Invalid or expired token"
}
```

---

### POST /api/password-reset/reset
Reset password using valid token.

**Request Body:**
```json
{
  "token": "abc123def456",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

**Response:** `200 OK`
```json
{
  "message": "Your password has been successfully reset. You can now log in with your new password."
}
```

**Validations:**
- token: Required, must be valid and not expired
- newPassword: 8+ chars, uppercase, lowercase, digit
- confirmPassword: Must match newPassword

**Business Logic:**
- Token marked as used after successful reset
- Account unlocked if locked
- Failed login attempts reset to 0

**Error Responses:**
- `400 BAD_REQUEST` - Invalid token, passwords don't match, or validation failed

---

## 📨 Request/Response Examples

### Example 1: Complete User Registration Flow

**Step 1: Register**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "Password123!",
    "confirmPassword": "Password123!",
    "fullname": "John Doe",
    "mobile": "1234567890"
  }'
```

**Step 2: Login**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "password": "Password123!"
  }'
```

**Step 3: Get Profile**
```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Example 2: Support Ticket Creation Flow

**Step 1: Create Ticket (Public)**
```bash
curl -X POST http://localhost:8080/api/support/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "fullname": "John Doe",
    "email": "john@example.com",
    "subject": "Payment Issue",
    "query": "Unable to process payment",
    "priority": "high"
  }'
```

**Step 2: Check Ticket Status (Public)**
```bash
curl -X GET http://localhost:8080/api/support/ticket/TKT-1730628000000
```

---

### Example 3: Password Reset Flow

**Step 1: Request Reset**
```bash
curl -X POST http://localhost:8080/api/password-reset/request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

**Step 2: Validate Token**
```bash
curl -X GET "http://localhost:8080/api/password-reset/validate?token=abc123def456"
```

**Step 3: Reset Password**
```bash
curl -X POST http://localhost:8080/api/password-reset/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "abc123def456",
    "newPassword": "NewPassword456!",
    "confirmPassword": "NewPassword456!"
  }'
```

---

## 🔒 Security Headers

All requests should include:

**For Public Endpoints:**
```
Content-Type: application/json
```

**For Protected Endpoints:**
```
Content-Type: application/json
Authorization: Bearer <jwt-token>
```

---

## ⚠️ Error Response Format

All errors follow a standard format:

```json
{
  "status": 400,
  "message": "Error description",
  "timestamp": "2025-11-03T10:30:00"
}
```

**Common HTTP Status Codes:**
- `200 OK` - Successful request
- `201 CREATED` - Resource created successfully
- `204 NO CONTENT` - Successful deletion
- `400 BAD_REQUEST` - Validation error or bad input
- `401 UNAUTHORIZED` - Authentication required or failed
- `403 FORBIDDEN` - Access denied (insufficient permissions)
- `404 NOT_FOUND` - Resource not found
- `409 CONFLICT` - Duplicate resource (username/email exists)
- `429 TOO_MANY_REQUESTS` - Rate limit exceeded
- `500 INTERNAL_SERVER_ERROR` - Server error

---

## 📊 Endpoint Summary

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/api/auth/register` | POST | Public | Register new user |
| `/api/auth/login` | POST | Public | User login |
| `/api/auth/refresh` | POST | Auth | Refresh JWT token |
| `/api/auth/logout` | POST | Auth | Logout user |
| `/api/auth/me` | GET | Auth | Get current user |
| `/api/users` | GET | Admin | Get all users |
| `/api/users/{id}` | GET | User/Admin | Get user by ID |
| `/api/users/profile` | GET | Auth | Get own profile |
| `/api/users/profile` | PUT | Auth | Update own profile |
| `/api/users/change-password` | POST | Auth | Change password |
| `/api/users/{id}/activate` | PUT | Admin | Activate user |
| `/api/users/{id}/deactivate` | PUT | Admin | Deactivate user |
| `/api/users/{id}/lock` | PUT | Admin | Lock user account |
| `/api/users/{id}/unlock` | PUT | Admin | Unlock user account |
| `/api/users/{id}` | DELETE | Admin | Delete user |
| `/api/support/tickets` | POST | Public | Create ticket |
| `/api/support/tickets` | GET | User/Admin | Get tickets |
| `/api/support/tickets/{id}` | GET | User/Admin | Get ticket by ID |
| `/api/support/tickets/{id}` | PUT | Admin | Update ticket |
| `/api/support/ticket/{ticketNumber}` | GET | Public | Get by ticket number |
| `/api/password-reset/request` | POST | Public | Request reset |
| `/api/password-reset/validate` | GET | Public | Validate token |
| `/api/password-reset/reset` | POST | Public | Reset password |

**Total Endpoints**: 24  
**Public Endpoints**: 8  
**Authenticated Endpoints**: 6  
**Admin-Only Endpoints**: 10

---

**Last Updated**: November 3, 2025  
**API Version**: 1.0  
**Author**: Raghul
