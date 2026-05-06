# Rawgul Spring Boot Web Application

A production-ready Spring Boot 3.x web application with MySQL database integration, JWT authentication, and comprehensive security features.

## Technology Stack

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **Database**: MySQL
- **Server**: Apache Tomcat (Embedded)
- **Security**: Spring Security + JWT
- **Build Tool**: Maven
- **ORM**: Spring Data JPA / Hibernate

## Project Structure

```
src/
├── main/
│   ├── java/com/rawgul/
│   │   ├── Application.java                 # Main application class
│   │   ├── config/                          # Configuration classes
│   │   │   ├── SecurityConfig.java          # Security configuration
│   │   │   └── WebConfig.java               # Web/CORS configuration
│   │   ├── controller/                      # REST controllers
│   │   │   ├── AuthController.java          # Authentication endpoints
│   │   │   └── UserController.java          # User management endpoints
│   │   ├── dto/                             # Data Transfer Objects
│   │   │   ├── LoginRequest.java
│   │   │   ├── SignupRequest.java
│   │   │   ├── JwtResponse.java
│   │   │   └── MessageResponse.java
│   │   ├── exceptions/                      # Exception handling
│   │   │   ├── ErrorResponse.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceAlreadyExistsException.java
│   │   │   └── ResourceNotFoundException.java
│   │   ├── model/                           # Entity classes
│   │   │   ├── User.java
│   │   │   └── Role.java
│   │   ├── repository/                      # Data access layer
│   │   │   ├── UserRepository.java
│   │   │   └── RoleRepository.java
│   │   ├── security/                        # Security components
│   │   │   ├── AuthEntryPointJwt.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── UserDetailsImpl.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   └── service/                         # Business logic layer
│   │       ├── AuthService.java
│   │       └── UserService.java
│   └── resources/
│       ├── application.properties           # Main configuration
│       ├── application-dev.properties       # Development profile
│       └── application-prod.properties      # Production profile
└── test/
    └── java/com/rawgul/
        └── ApplicationTests.java            # Test classes
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Database Setup

### Option 1: Automatic Setup (Recommended for Development)

1. Create a MySQL database:
```sql
CREATE DATABASE finance_tracker_db;
```

2. Update database credentials in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/finance_tracker_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

3. Run the application - Hibernate will create tables automatically

4. Initialize roles (run after first startup):
```sql
INSERT INTO roles(name) VALUES('ROLE_USER');
INSERT INTO roles(name) VALUES('ROLE_ADMIN');
INSERT INTO roles(name) VALUES('ROLE_MODERATOR');
```

### Option 2: Manual Setup (Recommended for Production)

1. Use the provided SQL schema file:
```bash
mysql -u your_username -p < database-schema.sql
```

2. Update `application.properties` with production settings:
```properties
spring.jpa.hibernate.ddl-auto=validate
```

See `database-schema.sql` for the complete schema definition.

## Configuration

### JWT Secret Key
⚠️ **IMPORTANT**: Change the JWT secret in `application.properties`:
```properties
jwt.secret=your_secure_secret_key_minimum_256_bits
```

Generate a secure secret:
```bash
openssl rand -base64 64
```

### Application Profiles

- **Development**: `application-dev.properties`
- **Production**: `application-prod.properties`

Run with specific profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Building the Application

### Using Maven Wrapper (Recommended)
```bash
./mvnw clean install
```

### Using System Maven
```bash
mvn clean install
```

## Running the Application

### Development Mode
```bash
./mvnw spring-boot:run
```

### Production Mode
```bash
java -jar target/web-application-1.0.0.jar --spring.profiles.active=prod
```

The application will start on: `http://localhost:8080`

## API Endpoints

### Authentication & User Management

#### Register User (Sign Up)
```http
POST /api/auth/signup
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "fullname": "Test User",
  "mobile": "+1234567890",
  "roles": ["user"]
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "password123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "roles": ["ROLE_USER"]
}
```

### Password Reset (Forgot Password)

#### Initiate Password Reset
```http
POST /api/auth/forgot-password
Content-Type: application/json

{
  "email": "test@example.com",
  "captcha": "answer"
}
```

#### Validate Reset Token
```http
GET /api/auth/reset-password/validate?token={token}
```

#### Confirm Password Reset
```http
POST /api/auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-here",
  "newPassword": "newpassword123",
  "confirmPassword": "newpassword123"
}
```

### Support Tickets

#### Create Support Ticket (Public - No Auth Required)
```http
POST /api/support/ticket
Content-Type: application/json

{
  "fullname": "John Doe",
  "email": "john@example.com",
  "subject": "Need help with account",
  "priority": "medium",
  "query": "I am having trouble accessing my account..."
}
```

#### Get Ticket by Number
```http
GET /api/support/ticket/TKT-1234567890
```

#### Get All Tickets (Admin Only)
```http
GET /api/support/tickets
Authorization: Bearer {token}
```

#### Get Tickets by Status (Admin Only)
```http
GET /api/support/tickets/status/OPEN
Authorization: Bearer {token}
```

#### Assign Ticket (Admin Only)
```http
PUT /api/support/ticket/{id}/assign?assignee=agent@example.com
Authorization: Bearer {token}
```

#### Resolve Ticket (Admin Only)
```http
PUT /api/support/ticket/{id}/resolve
Authorization: Bearer {token}
Content-Type: application/json

"Issue has been resolved by updating the user's account settings."
```

#### Close Ticket (Admin Only)
```http
PUT /api/support/ticket/{id}/close
Authorization: Bearer {token}
```

### User Management (Protected)

#### Get All Users (Admin Only)
```http
GET /api/users
Authorization: Bearer {token}
```

#### Get User by ID
```http
GET /api/users/{id}
Authorization: Bearer {token}
```

#### Delete User (Admin Only)
```http
DELETE /api/users/{id}
Authorization: Bearer {token}
```

## Entity Models

The application includes comprehensive entity models with proper JPA relationships:

### User Entity
- Complete user profile with audit fields
- Account locking after failed login attempts
- Mobile number validation
- Bidirectional relationships with support tickets and password reset tokens

### SupportTicket Entity
- Full support ticket management system
- Auto-generated unique ticket numbers
- Status tracking (OPEN, IN_PROGRESS, PENDING_USER, RESOLVED, CLOSED)
- Priority levels (LOW, MEDIUM, HIGH, CRITICAL)
- Can be created by authenticated or anonymous users

### PasswordResetToken Entity
- Secure password reset functionality
- Time-limited tokens (24 hours)
- One-time use validation
- IP address and user agent tracking
- Automatic cleanup of expired tokens

For detailed entity documentation, see [ENTITY_DOCUMENTATION.md](ENTITY_DOCUMENTATION.md)

## Security

### Authentication
- JWT-based authentication
- Token expiration: 24 hours (configurable)
- BCrypt password encoding
- Account lockout after 5 failed login attempts

### Authorization
- Role-based access control (RBAC)
- Roles: USER, ADMIN, MODERATOR
- Method-level security with `@PreAuthorize`

### Password Reset Security
- Secure token generation with UUID
- Time-limited tokens (24 hours)
- Rate limiting (max 3 requests per hour)
- IP address and user agent tracking
- One-time use enforcement

### CORS
- Configured for cross-origin requests
- Customizable in `WebConfig.java`

## Health Check

```http
GET /actuator/health
```

## Testing

Run tests:
```bash
./mvnw test
```

## Production Deployment

1. Build production JAR:
```bash
./mvnw clean package -Pprod
```

2. Set environment variables:
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://your-db-host:3306/your_db
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your_production_secret_key
```

3. Run application:
```bash
java -jar target/web-application-1.0.0.jar --spring.profiles.active=prod
```

## Common Issues

### Port Already in Use
Change port in `application.properties`:
```properties
server.port=8081
```

### Database Connection Failed
- Verify MySQL is running
- Check credentials in application.properties
- Ensure database exists

### JWT Token Issues
- Verify secret key is at least 256 bits
- Check token expiration time
- Ensure proper Authorization header format: `Bearer {token}`

## Features Overview

### Implemented Features
✅ User authentication with JWT
✅ User registration with validation
✅ Password reset with email tokens
✅ Support ticket system
✅ Role-based access control
✅ Account lockout after failed logins
✅ JPA auditing (created/modified timestamps)
✅ Scheduled cleanup of expired tokens
✅ Comprehensive validation
✅ Global exception handling

### Frontend Integration
- **login.html** → User entity, Login endpoint
- **sign-up.html** → User entity, Signup endpoint  
- **forgot-password.html** → PasswordResetToken entity, Reset endpoints
- **support.html** → SupportTicket entity, Support endpoints

## Development Tips

1. **Hot Reload**: Spring Boot DevTools is included for automatic restart
2. **Database Migration**: Consider adding Flyway or Liquibase for production
3. **API Documentation**: Add SpringDoc OpenAPI for Swagger documentation
4. **Logging**: Configured with SLF4J and Logback
5. **Validation**: Bean Validation enabled with Hibernate Validator
6. **Email Service**: Implement email service for password reset and ticket notifications
7. **Audit Logging**: JPA Auditing automatically tracks created/modified dates and users

## License

This project is licensed under the MIT License.

## Support

For issues and questions, please create an issue in the repository.
