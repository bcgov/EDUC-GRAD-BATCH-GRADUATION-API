package ca.bc.gov.educ.api.batchgraduation;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "1s")
public class EducGradBatchGraduationApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducGradBatchGraduationApplication.class, args);
    }

}
