/**
 * Form Validation Module
 * 
 * Comprehensive client-side form validation with real-time feedback,
 * password strength checking, and API integration.
 * 
 * Features:
 * - Real-time validation
 * - Email format validation
 * - Password strength checker (min 8 chars, uppercase, lowercase, number, special char)
 * - Password confirmation matching
 * - Display validation errors below fields
 * - Disable submit button until form is valid
 * - Loading state during API calls
 * - Custom validation rules
 * 
 * @author Raghul
 * @version 1.0
 * @since 2025-11-03
 */

// ==================== Validation Rules ====================

const ValidationRules = {
    // Email validation
    email: {
        pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
        message: 'Please enter a valid email address'
    },
    
    // Username validation (alphanumeric, underscore, 3-20 chars)
    username: {
        pattern: /^[a-zA-Z0-9_]{3,20}$/,
        message: 'Username must be 3-20 characters (letters, numbers, underscore only)'
    },
    
    // Mobile number validation (10 digits, optional country code)
    mobile: {
        pattern: /^(\+\d{1,3}[- ]?)?\d{10}$/,
        message: 'Please enter a valid 10-digit mobile number'
    },
    
    // Password requirements
    password: {
        minLength: 8,
        requireUppercase: true,
        requireLowercase: true,
        requireNumber: true,
        requireSpecial: true,
        specialChars: '!@#$%^&*()_+-=[]{}|;:,.<>?'
    },
    
    // Name validation (letters, spaces, hyphens)
    name: {
        pattern: /^[a-zA-Z\s'-]{2,50}$/,
        message: 'Name must be 2-50 characters (letters, spaces, hyphens only)'
    },
    
    // Subject validation (min 5 chars)
    subject: {
        minLength: 5,
        message: 'Subject must be at least 5 characters'
    },
    
    // Message validation (min 10 chars)
    message: {
        minLength: 10,
        message: 'Message must be at least 10 characters'
    }
};

// ==================== Validation Functions ====================

const Validator = {
    /**
     * Validates email format
     * @param {string} email - Email to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateEmail(email) {
        if (!email || email.trim() === '') {
            return { valid: false, message: 'Email is required' };
        }
        
        if (!ValidationRules.email.pattern.test(email.trim())) {
            return { valid: false, message: ValidationRules.email.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Validates username
     * @param {string} username - Username to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateUsername(username) {
        if (!username || username.trim() === '') {
            return { valid: false, message: 'Username is required' };
        }
        
        if (!ValidationRules.username.pattern.test(username.trim())) {
            return { valid: false, message: ValidationRules.username.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Validates mobile number
     * @param {string} mobile - Mobile number to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateMobile(mobile) {
        if (!mobile || mobile.trim() === '') {
            return { valid: false, message: 'Mobile number is required' };
        }
        
        if (!ValidationRules.mobile.pattern.test(mobile.trim())) {
            return { valid: false, message: ValidationRules.mobile.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Validates name
     * @param {string} name - Name to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateName(name) {
        if (!name || name.trim() === '') {
            return { valid: false, message: 'Name is required' };
        }
        
        if (!ValidationRules.name.pattern.test(name.trim())) {
            return { valid: false, message: ValidationRules.name.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Validates password strength
     * @param {string} password - Password to validate
     * @returns {Object} {valid: boolean, message: string, strength: string, score: number}
     */
    validatePassword(password) {
        if (!password) {
            return { 
                valid: false, 
                message: 'Password is required',
                strength: 'none',
                score: 0
            };
        }
        
        const rules = ValidationRules.password;
        const errors = [];
        let score = 0;
        
        // Check minimum length
        if (password.length < rules.minLength) {
            errors.push(`At least ${rules.minLength} characters`);
        } else {
            score += 20;
        }
        
        // Check uppercase
        if (rules.requireUppercase && !/[A-Z]/.test(password)) {
            errors.push('One uppercase letter');
        } else {
            score += 20;
        }
        
        // Check lowercase
        if (rules.requireLowercase && !/[a-z]/.test(password)) {
            errors.push('One lowercase letter');
        } else {
            score += 20;
        }
        
        // Check number
        if (rules.requireNumber && !/[0-9]/.test(password)) {
            errors.push('One number');
        } else {
            score += 20;
        }
        
        // Check special character
        const specialCharsRegex = new RegExp(`[${rules.specialChars.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}]`);
        if (rules.requireSpecial && !specialCharsRegex.test(password)) {
            errors.push('One special character (!@#$%^&*...)');
        } else {
            score += 20;
        }
        
        // Additional points for length
        if (password.length >= 12) score += 10;
        if (password.length >= 16) score += 10;
        
        // Determine strength
        let strength = 'weak';
        if (score >= 80) strength = 'strong';
        else if (score >= 60) strength = 'medium';
        
        const valid = errors.length === 0;
        const message = valid ? 'Password is strong' : 'Password must contain: ' + errors.join(', ');
        
        return { valid, message, strength, score };
    },
    
    /**
     * Validates password confirmation
     * @param {string} password - Original password
     * @param {string} confirmPassword - Confirmation password
     * @returns {Object} {valid: boolean, message: string}
     */
    validatePasswordMatch(password, confirmPassword) {
        if (!confirmPassword) {
            return { valid: false, message: 'Please confirm your password' };
        }
        
        if (password !== confirmPassword) {
            return { valid: false, message: 'Passwords do not match' };
        }
        
        return { valid: true, message: 'Passwords match' };
    },
    
    /**
     * Validates subject
     * @param {string} subject - Subject to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateSubject(subject) {
        if (!subject || subject.trim() === '') {
            return { valid: false, message: 'Subject is required' };
        }
        
        if (subject.trim().length < ValidationRules.subject.minLength) {
            return { valid: false, message: ValidationRules.subject.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Validates message/textarea
     * @param {string} message - Message to validate
     * @returns {Object} {valid: boolean, message: string}
     */
    validateMessage(message) {
        if (!message || message.trim() === '') {
            return { valid: false, message: 'Message is required' };
        }
        
        if (message.trim().length < ValidationRules.message.minLength) {
            return { valid: false, message: ValidationRules.message.message };
        }
        
        return { valid: true, message: '' };
    },
    
    /**
     * Generic required field validation
     * @param {string} value - Value to validate
     * @param {string} fieldName - Field name for error message
     * @returns {Object} {valid: boolean, message: string}
     */
    validateRequired(value, fieldName = 'This field') {
        if (!value || value.trim() === '') {
            return { valid: false, message: `${fieldName} is required` };
        }
        
        return { valid: true, message: '' };
    }
};

// ==================== UI Helper Functions ====================

const UIHelper = {
    /**
     * Shows validation error below input field
     * @param {HTMLElement} input - Input element
     * @param {string} message - Error message
     */
    showError(input, message) {
        // Remove existing error
        this.clearError(input);
        
        // Add error class to input
        input.classList.add('error');
        input.classList.remove('valid');
        
        // Create error message element
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        errorDiv.setAttribute('role', 'alert');
        
        // Insert error message after input
        const formGroup = input.closest('.form-group');
        if (formGroup) {
            formGroup.appendChild(errorDiv);
        } else {
            input.parentNode.insertBefore(errorDiv, input.nextSibling);
        }
        
        // Add shake animation
        input.classList.add('shake');
        setTimeout(() => input.classList.remove('shake'), 500);
    },
    
    /**
     * Shows success state for input field
     * @param {HTMLElement} input - Input element
     */
    showSuccess(input) {
        this.clearError(input);
        input.classList.remove('error');
        input.classList.add('valid');
    },
    
    /**
     * Clears validation state from input field
     * @param {HTMLElement} input - Input element
     */
    clearError(input) {
        input.classList.remove('error', 'valid', 'shake');
        
        const formGroup = input.closest('.form-group');
        if (formGroup) {
            const errorMessage = formGroup.querySelector('.error-message');
            if (errorMessage) {
                errorMessage.remove();
            }
        } else {
            const nextElement = input.nextSibling;
            if (nextElement && nextElement.classList && nextElement.classList.contains('error-message')) {
                nextElement.remove();
            }
        }
    },
    
    /**
     * Updates password strength indicator
     * @param {HTMLElement} container - Password strength container
     * @param {Object} result - Validation result with strength info
     */
    updatePasswordStrength(container, result) {
        if (!container) return;
        
        const bar = container.querySelector('.strength-bar-fill');
        const text = container.querySelector('.strength-text');
        const requirements = container.querySelector('.password-requirements');
        
        if (bar) {
            bar.style.width = `${result.score}%`;
            bar.className = 'strength-bar-fill';
            
            if (result.strength === 'weak') {
                bar.classList.add('weak');
            } else if (result.strength === 'medium') {
                bar.classList.add('medium');
            } else if (result.strength === 'strong') {
                bar.classList.add('strong');
            }
        }
        
        if (text) {
            text.textContent = result.strength.charAt(0).toUpperCase() + result.strength.slice(1);
            text.className = 'strength-text ' + result.strength;
        }
        
        if (requirements) {
            requirements.textContent = result.message;
        }
    },
    
    /**
     * Shows loading state on submit button
     * @param {HTMLElement} button - Submit button
     * @param {string} loadingText - Text to display while loading
     */
    showLoadingState(button, loadingText = 'Processing...') {
        button.disabled = true;
        button.dataset.originalText = button.innerHTML;
        button.innerHTML = `<i class="fas fa-spinner fa-spin"></i> ${loadingText}`;
        button.classList.add('loading');
    },
    
    /**
     * Hides loading state on submit button
     * @param {HTMLElement} button - Submit button
     */
    hideLoadingState(button) {
        button.disabled = false;
        if (button.dataset.originalText) {
            button.innerHTML = button.dataset.originalText;
        }
        button.classList.remove('loading');
    },
    
    /**
     * Shows notification message
     * @param {string} message - Message to display
     * @param {string} type - Message type (success, error, warning, info)
     */
    showNotification(message, type = 'info') {
        // Remove existing notification
        const existing = document.querySelector('.notification-toast');
        if (existing) {
            existing.remove();
        }
        
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification-toast ${type}`;
        
        const icon = {
            success: 'fa-check-circle',
            error: 'fa-times-circle',
            warning: 'fa-exclamation-triangle',
            info: 'fa-info-circle'
        }[type] || 'fa-info-circle';
        
        notification.innerHTML = `
            <i class="fas ${icon}"></i>
            <span>${message}</span>
            <button class="close-notification" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        document.body.appendChild(notification);
        
        // Animate in
        setTimeout(() => notification.classList.add('show'), 10);
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            notification.classList.remove('show');
            setTimeout(() => notification.remove(), 300);
        }, 5000);
    }
};

// ==================== Form Validator Class ====================

class FormValidator {
    constructor(formId, options = {}) {
        this.form = document.getElementById(formId);
        if (!this.form) {
            console.error(`Form with id "${formId}" not found`);
            return;
        }
        
        this.options = {
            validateOnBlur: true,
            validateOnInput: true,
            disableSubmitUntilValid: true,
            showPasswordStrength: true,
            ...options
        };
        
        this.fields = {};
        this.isValid = false;
        
        this.init();
    }
    
    init() {
        // Setup real-time validation
        this.setupValidation();
        
        // Setup form submission
        this.form.addEventListener('submit', (e) => this.handleSubmit(e));
        
        // Initial validation state
        this.validateForm();
    }
    
    setupValidation() {
        // Get all inputs, textareas, and selects
        const fields = this.form.querySelectorAll('input:not([type="checkbox"]):not([type="radio"]), textarea, select');
        
        fields.forEach(field => {
            const fieldName = field.name || field.id;
            
            if (this.options.validateOnBlur) {
                field.addEventListener('blur', () => this.validateField(field));
            }
            
            if (this.options.validateOnInput) {
                field.addEventListener('input', () => {
                    // Debounce validation for better performance
                    clearTimeout(field.validationTimeout);
                    field.validationTimeout = setTimeout(() => {
                        this.validateField(field);
                        this.validateForm();
                    }, 300);
                });
            }
            
            this.fields[fieldName] = field;
        });
        
        // Setup password strength indicator if exists
        if (this.options.showPasswordStrength) {
            this.setupPasswordStrength();
        }
    }
    
    setupPasswordStrength() {
        const passwordField = this.form.querySelector('#password, input[name="password"]');
        if (!passwordField) return;
        
        // Check if strength indicator already exists
        let strengthContainer = passwordField.parentElement.querySelector('.password-strength');
        
        if (!strengthContainer) {
            // Create password strength indicator
            strengthContainer = document.createElement('div');
            strengthContainer.className = 'password-strength';
            strengthContainer.innerHTML = `
                <div class="strength-bar">
                    <div class="strength-bar-fill"></div>
                </div>
                <div class="strength-info">
                    <span class="strength-text">Weak</span>
                    <span class="password-requirements"></span>
                </div>
            `;
            
            passwordField.parentElement.appendChild(strengthContainer);
        }
        
        // Update strength on input
        passwordField.addEventListener('input', () => {
            const result = Validator.validatePassword(passwordField.value);
            UIHelper.updatePasswordStrength(strengthContainer, result);
        });
    }
    
    validateField(field) {
        const fieldName = field.name || field.id;
        const value = field.value;
        let result = { valid: true, message: '' };
        
        // Skip validation for hidden or disabled fields
        if (field.type === 'hidden' || field.disabled) {
            return true;
        }
        
        // Validate based on field type/name
        if (fieldName.includes('email')) {
            result = Validator.validateEmail(value);
        } else if (fieldName.includes('username')) {
            result = Validator.validateUsername(value);
        } else if (fieldName.includes('mobile') || fieldName.includes('phone')) {
            result = Validator.validateMobile(value);
        } else if (fieldName.includes('password') && !fieldName.includes('confirm')) {
            result = Validator.validatePassword(value);
        } else if (fieldName.includes('confirm')) {
            const passwordField = this.form.querySelector('#password, input[name="password"]');
            result = Validator.validatePasswordMatch(passwordField?.value, value);
        } else if (fieldName.includes('name') || fieldName.includes('fullname')) {
            result = Validator.validateName(value);
        } else if (fieldName.includes('subject')) {
            result = Validator.validateSubject(value);
        } else if (field.tagName === 'TEXTAREA' || fieldName.includes('message') || fieldName.includes('query')) {
            result = Validator.validateMessage(value);
        } else if (field.required) {
            result = Validator.validateRequired(value, field.placeholder || fieldName);
        }
        
        // Update UI
        if (!result.valid) {
            UIHelper.showError(field, result.message);
            return false;
        } else {
            UIHelper.showSuccess(field);
            return true;
        }
    }
    
    validateForm() {
        let allValid = true;
        
        // Validate all required fields
        Object.values(this.fields).forEach(field => {
            if (field.required && !field.disabled) {
                const isValid = this.validateField(field);
                if (!isValid) {
                    allValid = false;
                }
            }
        });
        
        // Check password match if both password fields exist
        const passwordField = this.form.querySelector('#password, input[name="password"]');
        const confirmField = this.form.querySelector('#confirm-password, input[name="confirm-password"]');
        
        if (passwordField && confirmField && confirmField.value) {
            const result = Validator.validatePasswordMatch(passwordField.value, confirmField.value);
            if (!result.valid) {
                allValid = false;
            }
        }
        
        // Update submit button state
        const submitButton = this.form.querySelector('button[type="submit"]');
        if (submitButton && this.options.disableSubmitUntilValid) {
            submitButton.disabled = !allValid;
        }
        
        this.isValid = allValid;
        return allValid;
    }
    
    handleSubmit(e) {
        e.preventDefault();
        
        // Final validation
        if (!this.validateForm()) {
            UIHelper.showNotification('Please fix all errors before submitting', 'error');
            
            // Focus first invalid field
            const firstInvalid = this.form.querySelector('.error');
            if (firstInvalid) {
                firstInvalid.focus();
            }
            
            return false;
        }
        
        // Show loading state
        const submitButton = this.form.querySelector('button[type="submit"]');
        UIHelper.showLoadingState(submitButton);
        
        // Get form data
        const formData = new FormData(this.form);
        const data = Object.fromEntries(formData.entries());
        
        // Call custom submit handler if provided
        if (this.options.onSubmit) {
            this.options.onSubmit(data, this.form)
                .then(() => {
                    UIHelper.hideLoadingState(submitButton);
                })
                .catch(error => {
                    UIHelper.hideLoadingState(submitButton);
                    UIHelper.showNotification(error.message || 'An error occurred', 'error');
                });
        } else {
            // Default: just submit the form
            setTimeout(() => {
                this.form.submit();
            }, 500);
        }
    }
    
    reset() {
        this.form.reset();
        Object.values(this.fields).forEach(field => {
            UIHelper.clearError(field);
        });
        this.validateForm();
    }
}

// ==================== Export ====================

// Global export
if (typeof window !== 'undefined') {
    window.FormValidator = FormValidator;
    window.Validator = Validator;
    window.UIHelper = UIHelper;
    window.ValidationRules = ValidationRules;
}

// ES6 export
export { FormValidator, Validator, UIHelper, ValidationRules };
