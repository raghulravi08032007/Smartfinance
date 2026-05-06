# Email Service Documentation

## Overview
The Email Service provides comprehensive email notification functionality for the Finance Tracker application. It supports both HTML template-based emails (using Thymeleaf) and plain text fallback emails.

## Features

### ✅ Email Types
1. **Welcome Email** - Sent to newly registered users
2. **Password Reset Email** - Sent when users request password reset
3. **Support Ticket Confirmation** - Sent when users submit support tickets
4. **Ticket Status Update** - Sent when support ticket status changes
5. **Generic Notification Email** - For custom notifications

### ✅ Key Capabilities
- **Asynchronous Sending** - All emails sent using `@Async` for non-blocking operations
- **HTML Templates** - Professional, responsive email templates using Thymeleaf
- **Fallback Support** - Automatic fallback to plain text if HTML fails
- **Error Handling** - Comprehensive logging and error handling
- **Template Variables** - Dynamic content injection using Thymeleaf
- **Responsive Design** - Mobile-friendly email templates

## Configuration

### 1. Application Properties

Add the following to `application.properties`:

```properties
# Email Configuration (SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_specific_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email Sender Configuration
app.email.from=noreply@financetracker.com
app.email.fromName=Finance Tracker Team
app.email.support=support@financetracker.com

# Thymeleaf Configuration
spring.thymeleaf.cache=false
spring.thymeleaf.mode=HTML
spring.thymeleaf.encoding=UTF-8
```

### 2. Gmail Configuration

For Gmail, you need to:
1. Enable 2-Factor Authentication
2. Generate an App-Specific Password
3. Use the app password in `spring.mail.password`

**Steps:**
1. Go to Google Account Settings
2. Security → 2-Step Verification → App passwords
3. Generate password for "Mail"
4. Use generated password in application.properties

### 3. Other SMTP Providers

**Outlook/Office 365:**
```properties
spring.mail.host=smtp.office365.com
spring.mail.port=587
```

**SendGrid:**
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your_sendgrid_api_key
```

**Amazon SES:**
```properties
spring.mail.host=email-smtp.us-east-1.amazonaws.com
spring.mail.port=587
spring.mail.username=your_aws_smtp_username
spring.mail.password=your_aws_smtp_password
```

## Usage

### Inject EmailService

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final EmailService emailService;
    
    // Use in your methods
}
```

### Send Welcome Email

```java
// After user registration
User newUser = userRepository.save(user);
emailService.sendWelcomeEmail(newUser);
```

### Send Password Reset Email

```java
// When user requests password reset
String resetToken = generateResetToken();
int expirationHours = 24;
emailService.sendPasswordResetEmail(
    user.getEmail(), 
    user.getUsername(), 
    resetToken, 
    expirationHours
);
```

### Send Support Ticket Confirmation

```java
// After support ticket creation
SupportTicket ticket = supportTicketRepository.save(newTicket);
emailService.sendSupportTicketConfirmation(ticket);
```

### Send Ticket Status Update

```java
// When ticket status changes
ticket.setStatus(TicketStatus.RESOLVED);
supportTicketRepository.save(ticket);
emailService.sendTicketStatusUpdate(ticket, "Issue resolved successfully");
```

### Send Generic Notification

```java
// For custom notifications
emailService.sendNotificationEmail(
    "user@example.com",
    "Important Update",
    "Your account has been updated successfully."
);
```

## Email Templates

### Template Location
All email templates are located in: `src/main/resources/templates/email/`

### Available Templates
1. `welcome-email.html` - Welcome email for new users
2. `password-reset-email.html` - Password reset request
3. `support-ticket-confirmation.html` - Support ticket confirmation
4. `ticket-status-update.html` - Ticket status change notification

### Template Variables

**Welcome Email:**
- `${username}` - User's username
- `${email}` - User's email
- `${registrationDate}` - Account creation date
- `${supportEmail}` - Support contact email

**Password Reset Email:**
- `${username}` - User's username
- `${resetUrl}` - Password reset link
- `${expirationHours}` - Token validity period
- `${supportEmail}` - Support contact email

**Support Ticket Confirmation:**
- `${name}` - Customer name
- `${ticketNumber}` - Unique ticket number
- `${subject}` - Ticket subject
- `${message}` - Ticket message
- `${status}` - Ticket status
- `${priority}` - Ticket priority
- `${createdDate}` - Creation timestamp
- `${supportEmail}` - Support contact email

**Ticket Status Update:**
- `${name}` - Customer name
- `${ticketNumber}` - Unique ticket number
- `${subject}` - Ticket subject
- `${status}` - New status
- `${updatedDate}` - Update timestamp
- `${notes}` - Status update notes
- `${supportEmail}` - Support contact email

## Customization

### Modify Email Templates

1. Edit HTML files in `src/main/resources/templates/email/`
2. Use Thymeleaf syntax for dynamic content
3. Maintain responsive CSS inline styles
4. Test across different email clients

### Add New Email Type

```java
@Async
public void sendNewEmailType(User user, String additionalData) {
    try {
        log.info("Sending new email type to: {}", user.getEmail());

        Context context = new Context();
        context.setVariable("username", user.getUsername());
        context.setVariable("data", additionalData);
        
        String htmlContent = templateEngine.process("email/new-email-template", context);
        
        sendHtmlEmail(
            user.getEmail(),
            "Email Subject",
            htmlContent
        );
        
        log.info("Email sent successfully");
    } catch (Exception e) {
        log.error("Failed to send email", e);
    }
}
```

## Async Configuration

The `AsyncConfig` class configures the thread pool for async email sending:

```java
- Core Pool Size: 5 threads
- Max Pool Size: 10 threads
- Queue Capacity: 100 tasks
- Thread Name Prefix: "async-email-"
- Graceful Shutdown: 60 seconds wait
```

### Monitoring Async Tasks

```java
// Check logs for async execution
2025-11-03 10:30:45 - Sending welcome email to: user@example.com
2025-11-03 10:30:46 - Welcome email sent successfully to: user@example.com
```

## Testing

### Test Email Service

```java
@SpringBootTest
class EmailServiceTest {
    
    @Autowired
    private EmailService emailService;
    
    @Test
    void testWelcomeEmail() {
        User user = User.builder()
            .username("testuser")
            .email("test@example.com")
            .build();
            
        emailService.sendWelcomeEmail(user);
        
        // Wait for async execution
        Thread.sleep(2000);
        
        // Check logs or email provider
    }
}
```

### Test SMTP Connection

```java
@SpringBootTest
class SmtpConnectionTest {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Test
    void testSmtpConnection() throws Exception {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@financetracker.com");
        message.setTo("test@example.com");
        message.setSubject("SMTP Test");
        message.setText("If you receive this, SMTP is working!");
        
        mailSender.send(message);
    }
}
```

## Troubleshooting

### Common Issues

**1. Authentication Failed**
- Check username and password in application.properties
- For Gmail, ensure app-specific password is used
- Verify 2FA is enabled for Gmail

**2. Connection Timeout**
- Check firewall settings
- Verify SMTP port is not blocked
- Test network connectivity to SMTP server

**3. SSL/TLS Errors**
- Ensure `starttls.enable=true`
- Check `ssl.trust` property
- Verify SMTP server supports TLS

**4. Emails Going to Spam**
- Add SPF record to domain DNS
- Set up DKIM authentication
- Configure domain verification with email provider
- Use verified sender email address

**5. Template Not Found**
- Check template path: `templates/email/template-name.html`
- Verify Thymeleaf configuration
- Check for typos in template name

### Enable Debug Logging

Add to application.properties:

```properties
logging.level.org.springframework.mail=DEBUG
logging.level.com.rawgul.service.EmailService=DEBUG
```

## Production Considerations

### Security
- Use environment variables for sensitive data
- Never commit SMTP credentials to version control
- Use secure email provider (SendGrid, AWS SES)
- Enable TLS/SSL for SMTP connections

### Performance
- Email sending is async (non-blocking)
- Configure appropriate thread pool size
- Monitor email queue capacity
- Set reasonable timeout values

### Reliability
- Implement retry mechanism for failed emails
- Log all email attempts and failures
- Monitor email delivery rates
- Set up alerts for email failures

### Scaling
- Consider dedicated email service (SendGrid, AWS SES)
- Implement rate limiting for email sending
- Use email queuing system (RabbitMQ, Kafka)
- Monitor email service costs

## Best Practices

1. **Always use async** - Don't block user requests
2. **Implement fallback** - Have plain text alternatives
3. **Log everything** - Track email success/failure
4. **Test thoroughly** - Verify across email clients
5. **Monitor delivery** - Track bounce rates and failures
6. **Respect privacy** - Include unsubscribe options
7. **Be responsive** - Design mobile-friendly templates
8. **Verify recipients** - Validate email addresses
9. **Handle errors gracefully** - Don't crash on email failure
10. **Keep templates updated** - Match current branding

## Example Integration

### In AuthService (User Registration)

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public User registerUser(RegisterRequest request) {
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();
            
        user = userRepository.save(user);
        
        // Send welcome email asynchronously
        emailService.sendWelcomeEmail(user);
        
        return user;
    }
}
```

### In PasswordResetService

```java
@Service
@RequiredArgsConstructor
public class PasswordResetService {
    
    private final EmailService emailService;
    
    @Value("${password.reset.token.expiration.hours}")
    private int expirationHours;
    
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            
        String resetToken = generateResetToken();
        
        // Send password reset email
        emailService.sendPasswordResetEmail(
            user.getEmail(),
            user.getUsername(),
            resetToken,
            expirationHours
        );
    }
}
```

### In SupportTicketService

```java
@Service
@RequiredArgsConstructor
public class SupportTicketService {
    
    private final SupportTicketRepository ticketRepository;
    private final EmailService emailService;
    
    public SupportTicket createTicket(SupportTicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
            .subject(request.getSubject())
            .message(request.getMessage())
            .fullName(request.getFullName())
            .contactEmail(request.getContactEmail())
            .ticketNumber(generateTicketNumber())
            .build();
            
        ticket = ticketRepository.save(ticket);
        
        // Send confirmation email
        emailService.sendSupportTicketConfirmation(ticket);
        
        return ticket;
    }
    
    public SupportTicket updateTicketStatus(Long id, TicketStatus status, String notes) {
        SupportTicket ticket = ticketRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
            
        ticket.setStatus(status);
        ticket = ticketRepository.save(ticket);
        
        // Send status update email
        emailService.sendTicketStatusUpdate(ticket, notes);
        
        return ticket;
    }
}
```

## Support

For issues or questions about the Email Service:
- Check logs: `logs/application.log`
- Review SMTP configuration
- Test email delivery manually
- Contact: support@financetracker.com

---

**Last Updated:** November 3, 2025  
**Version:** 1.0.0  
**Author:** Finance Tracker Development Team
