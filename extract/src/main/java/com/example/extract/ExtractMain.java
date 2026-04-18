package com.example.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Main class for Extract module
 */
public class ExtractMain {
    
    private static final Logger logger = LoggerFactory.getLogger(ExtractMain.class);
    
    public static void main(String[] args) {
        logger.info("Starting Extract Module...");
        
        try {
            // Load configuration
            Properties props = new Properties();
            InputStream input = ExtractMain.class.getClassLoader().getResourceAsStream("extract.properties");
            if (input == null) {
                throw new RuntimeException("extract.properties not found in classpath");
            }
            props.load(input);
            input.close();
            
            // Create extractor
            MySQLBinlogExtractor extractor = new MySQLBinlogExtractor();
            extractor.initialize(props);
            
            // Process all binlog files
            extractor.processAllFiles();
            
            logger.info("Extract Module completed successfully");
            
        } catch (Exception e) {
            logger.error("Error in Extract Module", e);
            System.exit(1);
        }
    }
}
