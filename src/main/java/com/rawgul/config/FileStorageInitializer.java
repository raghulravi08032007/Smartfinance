package com.rawgul.config;

import com.rawgul.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Initializes file storage system on application startup.
 * Creates necessary upload directories and performs any required setup.
 */
@Component
public class FileStorageInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageInitializer.class);

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Called when application is fully started and ready to serve requests.
     * Initializes the file storage system.
     *
     * @param event Application ready event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            logger.info("Initializing file storage system...");
            
            // Initialize file storage - creates upload directories
            fileStorageService.init();
            
            logger.info("File storage system initialized successfully");
            logger.info("Upload directory is ready to accept files");
            
        } catch (Exception e) {
            logger.error("Failed to initialize file storage system: {}", e.getMessage(), e);
            logger.warn("File upload functionality may not work properly");
            logger.warn("Please check file.upload.dir configuration in application.properties");
            // Don't throw exception - allow application to start even if file storage init fails
        }
    }
}
