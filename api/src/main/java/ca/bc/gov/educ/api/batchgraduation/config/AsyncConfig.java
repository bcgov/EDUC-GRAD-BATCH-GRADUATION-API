package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfig {
    @Bean(name = "asyncExecutor")
    public TaskExecutor asyncExecutor(EducGradBatchGraduationApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(constants.getNumberOfPartitions());
        executor.setMaxPoolSize(constants.getNumberOfPartitions());
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
