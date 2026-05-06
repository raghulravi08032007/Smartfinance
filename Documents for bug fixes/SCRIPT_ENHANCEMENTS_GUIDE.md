# 🚀 Script.js Enhancements Guide

## 📋 Overview

This document describes the comprehensive enhancements made to `script.js` to include authentication-aware features, user profile management, support ticket history, and improved notifications.

---

## ✨ New Features

### 1. **Authentication-Aware Navigation** 🔐

#### Dynamic Navigation Updates
The navigation now automatically adjusts based on user authentication status:

**When User is Logged In:**
- ✅ Hides Login/Sign-up links
- ✅ Shows Logout button
- ✅ Displays user greeting with username
- ✅ Shows profile link

**When User is Logged Out:**
- ✅ Shows Login/Sign-up links
- ✅ Hides user-specific elements
- ✅ Redirects protected pages to login

#### Key Functions:
```javascript
updateNavigationForAuth()  // Updates nav based on auth status
createLogoutButton()       // Creates dynamic logout button
handleLogout()             // Handles user logout with API call
```

---

### 2. **User Information Display** 👤

#### User Info Card
Displays comprehensive user information in a beautiful card:

```html
<div id="user-info-section"></div>
```

**Displays:**
- 👤 Full Name
- 📧 Email Address
- 🆔 Username
- 📱 Mobile Number (if available)
- 📅 Member Since Date

**Features:**
- Beautiful gradient background
- Avatar icon
- Action buttons (Edit Profile, Change Password)

#### Dynamic Greeting Message
Shows personalized greeting based on time of day:

```html
<div id="dynamic-greeting"></div>
```

**Examples:**
- 🌅 "Good morning, John!"
- ☀️ "Good afternoon, Sarah!"
- 🌙 "Good evening, Mike!"

---

### 3. **Profile Management** ⚙️

#### Edit Profile Modal
Full-featured profile editing with validation:

```javascript
openEditProfile()      // Opens edit profile modal
handleProfileUpdate()  // Saves profile changes
closeEditProfile()     // Closes modal
```

**Editable Fields:**
- Full Name
- Email Address
- Mobile Number

**Features:**
- ✅ Real-time validation
- ✅ Loading states during save
- ✅ Success/error notifications
- ✅ API integration with `apiService.user.updateProfile()`
- ✅ Local user data update

#### Change Password Modal
Secure password change functionality:

```javascript
openChangePassword()      // Opens password change modal
handlePasswordChange()    // Changes password with validation
closeChangePassword()     // Closes modal
```

**Password Requirements:**
- Current password verification
- Minimum 8 characters
- Match confirmation
- Strength requirements (uppercase, lowercase, number, special)

**Security:**
- ✅ Current password validation
- ✅ Password confirmation matching
- ✅ Automatic logout after change
- ✅ API integration with `apiService.user.changePassword()`

---

### 4. **Support Ticket History** 🎫

#### Ticket Management Dashboard
Comprehensive ticket tracking and management:

```html
<div id="support-tickets-container"></div>
```

```javascript
loadSupportTickets()     // Loads all user tickets
displayTicketsTable()    // Renders tickets in table
viewTicketDetails()      // Shows detailed ticket view
```

#### Ticket Table Features:

| Column | Description |
|--------|-------------|
| **Ticket #** | Unique ticket number with # prefix |
| **Subject** | Brief description of the issue |
| **Priority** | LOW / MEDIUM / HIGH badge |
| **Status** | Current ticket status with icon |
| **Created** | Formatted creation date |
| **Actions** | View details button |

#### Status Badges
Color-coded status indicators with icons:

- 🔵 **Open** - Blue gradient
- 🟡 **In Progress** - Orange gradient
- 🟢 **Resolved** - Green gradient
- ⚫ **Closed** - Gray gradient
- 🔴 **Pending** - Pink gradient

#### Priority Badges
Visual priority indicators:

- 🟢 **LOW** - Green gradient
- 🟡 **MEDIUM** - Orange gradient
- 🔴 **HIGH** - Red gradient

#### Ticket Details Modal
Comprehensive ticket information display:

**Shows:**
- Ticket number
- Subject
- Priority badge
- Status badge
- Creation date
- Full message content
- Support response (if available)

**Features:**
- Beautiful card layout
- Color-coded sections
- Responsive design
- Close button

---

### 5. **Enhanced Toast Notifications** 🔔

#### New Toast System
Professional notification system with rich features:

```javascript
showToast(message, type, duration)
```

**Types:**
- ✅ `success` - Green gradient with check icon
- ❌ `error` - Red gradient with exclamation icon
- ⚠️ `warning` - Orange gradient with warning icon
- ℹ️ `info` - Blue gradient with info icon

#### Toast Features:

**Visual Elements:**
- Large icon on left
- Message text in center
- Close button on right
- Colored gradient background
- Backdrop blur effect
- Border with transparency

**Animations:**
- Slide-in from right with bounce
- Smooth slide-out on close
- Auto-dismiss after 5 seconds
- Hover effects on close button

**Example Usage:**
```javascript
// Success notification
showToast('Profile updated successfully!', 'success');

// Error notification
showToast('Failed to load data', 'error');

// Warning notification
showToast('Session expiring soon', 'warning');

// Info notification
showToast('Loading ticket details...', 'info');
```

---

## 🔧 Integration Requirements

### Required Dependencies

#### JavaScript Files
Include in this order before `</body>`:

```html
<script src="assets/apiService.js"></script>
<script src="assets/auth.js"></script>
<script src="assets/script.js"></script>
```

#### CSS Files
Include in `<head>`:

```html
<link rel="stylesheet" href="assets/auth-ui.css">
```

### Required HTML Elements

#### Navigation Elements
```html
<!-- User greeting (optional) -->
<div id="user-greeting"></div>

<!-- Logout button (created dynamically) -->
<a href="#" id="logout-btn" style="display: none;">Logout</a>

<!-- Profile link (optional) -->
<a href="#" id="user-profile-link" style="display: none;">Profile</a>
```

#### Dashboard/Profile Page Elements
```html
<!-- Dynamic greeting -->
<div id="dynamic-greeting"></div>

<!-- User info section -->
<div id="user-info-section"></div>

<!-- Support tickets container -->
<div id="support-tickets-container"></div>
```

---

## 📊 API Integration

### Required API Methods

#### User Management
```javascript
// From apiService.user
apiService.user.updateProfile(data)
apiService.user.changePassword(data)
```

#### Support Tickets
```javascript
// From apiService.support
apiService.support.getMyTickets()
apiService.support.getTicketById(ticketId)
```

#### Authentication
```javascript
// From AuthManager
AuthManager.isAuthenticated()
AuthManager.logout()

// From UserManager
UserManager.getUser()
UserManager.updateUser(data)
```

---

## 🎨 Styling Guide

### CSS Classes

#### User Components
```css
.user-info-card         /* Main user info container */
.user-avatar            /* Avatar icon container */
.user-details           /* User information section */
.user-actions           /* Action buttons container */
#user-greeting          /* Navigation greeting */
#dynamic-greeting       /* Page header greeting */
.username-highlight     /* Highlighted username */
```

#### Modal Components
```css
.modal-overlay          /* Dark backdrop */
.modal-content          /* Modal container */
.modal-header           /* Modal header */
.modal-body             /* Modal content */
.modal-footer           /* Modal footer */
.close-btn              /* Close button */
```

#### Form Components
```css
.form-group             /* Form field container */
.form-actions           /* Form buttons container */
```

#### Ticket Components
```css
.tickets-table-wrapper  /* Table container */
.tickets-table          /* Main table */
.status-badge           /* Status indicator */
.priority-badge         /* Priority indicator */
.btn-icon               /* Icon button */
.ticket-details         /* Details container */
.ticket-info-grid       /* Info grid layout */
.message-content        /* Message display */
.response-content       /* Response display */
```

#### Utility Classes
```css
.btn                    /* Base button */
.btn-primary            /* Primary button */
.btn-secondary          /* Secondary button */
.loading-spinner        /* Loading indicator */
.no-tickets             /* Empty state */
.error-message          /* Error state */
```

---

## 🔄 Usage Examples

### 1. Initialize Authentication UI
```javascript
// Automatically runs on page load
document.addEventListener('DOMContentLoaded', function() {
  updateNavigationForAuth();
});
```

### 2. Display User Info
```javascript
// Get current user
const user = UserManager.getUser();

// Display user information
displayUserInfo(user);

// Update greeting
updateGreetingMessage(user);
```

### 3. Load Support Tickets
```javascript
// Load tickets on page load
if (document.getElementById('support-tickets-container')) {
  loadSupportTickets();
}
```

### 4. Open Profile Modals
```javascript
// Edit profile
<button onclick="openEditProfile()">Edit Profile</button>

// Change password
<button onclick="openChangePassword()">Change Password</button>
```

### 5. View Ticket Details
```javascript
// View ticket by ID
<button onclick="viewTicketDetails('TICKET-001')">View</button>
```

### 6. Show Notifications
```javascript
// Success notification
showToast('Profile updated!', 'success');

// Error notification
showToast('Update failed', 'error');

// Warning notification
showToast('Session expiring', 'warning');

// Info notification
showToast('Loading data...', 'info');
```

---

## 🎯 Best Practices

### Authentication
1. ✅ Always check `AuthManager.isAuthenticated()` before showing protected content
2. ✅ Update navigation on every page load
3. ✅ Handle token expiration gracefully
4. ✅ Show appropriate error messages

### User Data
1. ✅ Validate all input fields
2. ✅ Show loading states during API calls
3. ✅ Update local storage after successful updates
4. ✅ Refresh UI after data changes

### Notifications
1. ✅ Use appropriate notification types
2. ✅ Keep messages concise and clear
3. ✅ Don't spam multiple notifications
4. ✅ Allow users to dismiss notifications

### Performance
1. ✅ Load tickets only when needed
2. ✅ Cache user data locally
3. ✅ Debounce API calls
4. ✅ Use pagination for large datasets

---

## 🐛 Troubleshooting

### Issue: Navigation doesn't update after login
**Solution:** Call `updateNavigationForAuth()` after successful login

### Issue: User info not displaying
**Solution:** Ensure `#user-info-section` element exists and user is authenticated

### Issue: Tickets not loading
**Solution:** Check if `apiService.support.getMyTickets()` is available and user is authenticated

### Issue: Modals not closing
**Solution:** Ensure modal has correct ID and close function is called

### Issue: Toasts not appearing
**Solution:** Check if toast styles are loaded and element is appended to body

### Issue: Profile update fails
**Solution:** Validate form data and check API response for errors

---

## 📱 Responsive Design

### Mobile Optimizations
- User greeting shows only icon on mobile
- Tables scroll horizontally on small screens
- Modals take 95% width on mobile
- Form buttons stack vertically
- Ticket details use single column layout

### Breakpoints
```css
@media (max-width: 768px) {
  /* Mobile styles */
}
```

---

## ♿ Accessibility

### Features
- ✅ Keyboard navigation support
- ✅ ARIA labels on interactive elements
- ✅ Focus indicators on all buttons
- ✅ Screen reader friendly
- ✅ High contrast mode support
- ✅ Reduced motion support

---

## 🔐 Security Considerations

1. **Authentication**: Always verify authentication before showing sensitive data
2. **Password Changes**: Require current password for verification
3. **Session Management**: Auto-logout on token expiration
4. **Data Validation**: Validate all user inputs
5. **Error Handling**: Don't expose sensitive error details

---

## 📈 Future Enhancements

### Planned Features
- [ ] Avatar upload functionality
- [ ] Notification preferences
- [ ] Multi-factor authentication
- [ ] Ticket attachments
- [ ] Real-time ticket updates
- [ ] Export ticket history
- [ ] Dark mode toggle
- [ ] Email notifications

---

## 📞 Support

For questions or issues:
- **Email:** rr2258@srmist.edu.in
- **Phone:** +91 96264 49078

---

**Version:** 2.0  
**Last Updated:** November 3, 2025  
**Status:** ✅ Production Ready
