package ca.bc.gov.educ.api.batchgraduation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.DefaultPropertiesPersister;

import java.io.*;
import java.util.Properties;

@Component
public class PropertiesWriter {

    public void writeToApplicationProperties() {

        try {
            Properties properties = new Properties();
            properties.setProperty("batch.regular.grad.job.enabled", "false");
            properties.setProperty("batch.regular.grad.job.cron", "0 20 14 * * *");

            File file = new File("batch.properties");
            OutputStream outputStream = new FileOutputStream( file );
            DefaultPropertiesPersister defaultPropertiesPersister = new DefaultPropertiesPersister();
            defaultPropertiesPersister.store(properties, outputStream, "Comment");



        } catch (Exception e ) {
            System.out.println("An error during writing to  database.properties");
            e.printStackTrace();
        }

    }
}
