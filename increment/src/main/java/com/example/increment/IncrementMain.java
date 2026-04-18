package com.example.increment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Main class for Increment module
 */
public class IncrementMain {
    
    private static final Logger logger = LoggerFactory.getLogger(IncrementMain.class);
    
    public static void main(String[] args) {
        logger.info("Starting Increment Module...");
        
        try {
            // Load configuration
            Properties props = new Properties();
            InputStream input = IncrementMain.class.getClassLoader().getResourceAsStream("increment.properties");
            if (input == null) {
                throw new RuntimeException("increment.properties not found in classpath");
            }
            props.load(input);
            input.close();
            
            // Create converter
            THLToSqlConverter converter = new THLToSqlConverter(props);
            
            // Start conversion and execution
            converter.start();
            
            logger.info("Increment Module completed successfully");
            logger.info("Statistics - Total: {}, Successful: {}, Failed: {}", 
                converter.getTotalEvents(), converter.getSuccessfulEvents(), converter.getFailedEvents());
            
        } catch (Exception e) {
            logger.error("Error in Increment Module", e);
            System.exit(1);
        }
    }
}
