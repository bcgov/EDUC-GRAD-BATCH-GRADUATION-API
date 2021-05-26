package ca.bc.gov.educ.api.batchgraduation;

import ca.bc.gov.educ.api.batchgraduation.entity.GradCourseRestrictionsEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.model.GradCourseRestrictions;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import org.modelmapper.ModelMapper;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class EducGradBatchGraduationApplication {

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(EducGradBatchGraduationApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.typeMap(ConvGradStudentEntity.class, ConvGradStudent.class);
        modelMapper.typeMap(ConvGradStudent.class, ConvGradStudentEntity.class);

        modelMapper.typeMap(GradCourseRestrictionsEntity.class, GradCourseRestrictions.class);
        modelMapper.typeMap(GradCourseRestrictions.class, GradCourseRestrictionsEntity.class);

        return modelMapper;
    }
}
