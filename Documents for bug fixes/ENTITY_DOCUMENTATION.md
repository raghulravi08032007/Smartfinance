# Entity Relationship Documentation

## Database Schema Overview

This Spring Boot application uses the following JPA entities with proper relationships and audit fields:

### 1. User Entity
**Table:** `users`

**Fields:**
- `id` (Long, Primary Key)
- `username` (String, unique, indexed)
- `email` (String, unique, indexed)
- `password_hash` (String)
- `first_name` (String)
- `last_name` (String)
- `mobile_number` (String)
- `active` (Boolean, indexed)
- `account_locked` (Boolean)
- `failed_login_attempts` (Integer)
- `last_login` (LocalDateTime)
- `created_date` (LocalDateTime, @CreatedDate)
- `updated_date` (LocalDateTime, @LastModifiedDate)
- `created_by` (String)
- `updated_by` (String)

**Relationships:**
- Many-to-Many with `Role` (through `user_roles` join table)
- One-to-Many with `SupportTicket` (user can have multiple tickets)
- One-to-Many with `PasswordResetToken` (user can have multiple tokens)

**Frontend Mapping:** `login.html`, `sign-up.html`

---

### 2. Role Entity
**Table:** `roles`

**Fields:**
- `id` (Long, Primary Key)
- `name` (RoleType enum: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR)

**Relationships:**
- Many-to-Many with `User`

---

### 3. SupportTicket Entity
**Table:** `support_tickets`

**Fields:**
- `id` (Long, Primary Key)
- `subject` (String, max 200)
- `message` (Text, max 5000)
- `status` (Enum: OPEN, IN_PROGRESS, PENDING_USER, RESOLVED, CLOSED)
- `priority` (Enum: LOW, MEDIUM, HIGH, CRITICAL)
- `full_name` (String)
- `contact_email` (String)
- `ticket_number` (String, unique, auto-generated)
- `resolved_date` (LocalDateTime)
- `resolution_notes` (Text)
- `assigned_to` (String)
- `created_date` (LocalDateTime, @CreatedDate)
- `updated_date` (LocalDateTime, @LastModifiedDate)
- `created_by` (String)
- `updated_by` (String)

**Relationships:**
- Many-to-One with `User` (optional, ticket can be created without authentication)

**Indexes:** status, priority, created_date, user_id

**Frontend Mapping:** `support.html`

**Business Logic:**
- Auto-generates unique ticket number (TKT-{timestamp})
- Status transitions: OPEN → IN_PROGRESS → RESOLVED → CLOSED
- Can be assigned to support agents
- Tracks resolution date and notes

---

### 4. PasswordResetToken Entity
**Table:** `password_reset_tokens`

**Fields:**
- `id` (Long, Primary Key)
- `token` (String, unique, auto-generated UUID)
- `expiry_date` (LocalDateTime, default: 24 hours)
- `used` (Boolean, default: false)
- `used_date` (LocalDateTime)
- `ip_address` (String, for security tracking)
- `user_agent` (String, for security tracking)
- `created_date` (LocalDateTime, @CreatedDate)

**Relationships:**
- Many-to-One with `User` (required)

**Indexes:** token, expiry_date, user_id

**Frontend Mapping:** `forgot-password.html`

**Business Logic:**
- Auto-generates secure random token
- Expires after 24 hours
- One-time use only
- Tracks usage for security
- Can be validated before use
- Scheduled cleanup of expired tokens

---

## Database Relationships Diagram

```
┌─────────────────┐
│      User       │
│  (users table)  │
└────────┬────────┘
         │
         ├──────────────────────────┐
         │                          │
         │ Many-to-Many             │ One-to-Many
         │                          │
         ▼                          ▼
┌─────────────────┐      ┌──────────────────────┐
│      Role       │      │   SupportTicket      │
│  (roles table)  │      │ (support_tickets)    │
└─────────────────┘      └──────────────────────┘
                                  
         │
         │ One-to-Many
         │
         ▼
┌──────────────────────────┐
│  PasswordResetToken      │
│ (password_reset_tokens)  │
└──────────────────────────┘
```

## API Endpoints

### Authentication & User Management
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - User login
- `GET /api/users` - Get all users (Admin)
- `GET /api/users/{id}` - Get user by ID
- `DELETE /api/users/{id}` - Delete user (Admin)

### Password Reset
- `POST /api/auth/forgot-password` - Initiate password reset
- `GET /api/auth/reset-password/validate?token={token}` - Validate token
- `POST /api/auth/reset-password` - Confirm password reset

### Support Tickets
- `POST /api/support/ticket` - Create support ticket (Public)
- `POST /api/support/ticket/user` - Create ticket (Authenticated)
- `GET /api/support/ticket/{ticketNumber}` - Get ticket by number
- `GET /api/support/tickets` - Get all tickets (Admin)
- `GET /api/support/tickets/status/{status}` - Get tickets by status (Admin)
- `PUT /api/support/ticket/{id}/assign` - Assign ticket (Admin)
- `PUT /api/support/ticket/{id}/resolve` - Resolve ticket (Admin)
- `PUT /api/support/ticket/{id}/close` - Close ticket (Admin)
- `GET /api/support/tickets/count/open` - Get open tickets count (Admin)

## JPA Auditing

All entities use Spring Data JPA auditing with:
- `@CreatedDate` - Automatically set creation timestamp
- `@LastModifiedDate` - Automatically updated on modification
- `@EntityListeners(AuditingEntityListener.class)` - Enable auditing

The `JpaAuditingConfig` class provides automatic auditor tracking based on the authenticated user.

## Security Features

1. **User Account Security:**
   - Password hashing with BCrypt
   - Account locking after 5 failed login attempts
   - Last login tracking

2. **Password Reset Security:**
   - Time-limited tokens (24 hours)
   - One-time use tokens
   - IP address and user agent tracking
   - Rate limiting (max 3 requests per hour)
   - Automatic cleanup of expired tokens

3. **Support Ticket Security:**
   - Can be created with or without authentication
   - Role-based access control for admin functions
   - Automatic ticket number generation

## Database Initialization

Run these SQL commands after first application startup:

```sql
-- Initialize roles
INSERT INTO roles(name) VALUES('ROLE_USER');
INSERT INTO roles(name) VALUES('ROLE_ADMIN');
INSERT INTO roles(name) VALUES('ROLE_MODERATOR');
```

## Lombok Annotations Used

- `@Data` - Generates getters, setters, toString, equals, and hashCode
- `@Builder` - Implements builder pattern
- `@NoArgsConstructor` - Generates no-args constructor
- `@AllArgsConstructor` - Generates all-args constructor
- `@RequiredArgsConstructor` - Generates constructor for final fields

## Validation Constraints

All entities use Jakarta Bean Validation:
- `@NotBlank` - Field cannot be null or empty
- `@NotNull` - Field cannot be null
- `@Email` - Valid email format
- `@Size` - String length constraints
- `@Pattern` - Regex pattern validation (e.g., phone numbers)
