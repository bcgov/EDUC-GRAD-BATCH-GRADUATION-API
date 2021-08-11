package ca.bc.gov.educ.api.batchgraduation;

import org.modelmapper.ModelMapper;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class EducGradBatchGraduationApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradBatchGraduationApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper;
    }
}
