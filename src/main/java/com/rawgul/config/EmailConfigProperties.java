package com.rawgul.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for email settings
 * Binds application.properties email configuration to Java object
 */
@Configuration
@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailConfigProperties {
    
    /**
     * From email address (e.g., noreply@financetracker.com)
     */
    private String from;
    
    /**
     * From display name (e.g., Finance Tracker Team)
     */
    private String fromName;
    
    /**
     * Support email address (e.g., support@financetracker.com)
     */
    private String support;
}
