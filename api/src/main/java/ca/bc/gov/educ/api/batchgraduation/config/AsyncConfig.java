package ca.bc.gov.educ.api.batchgraduation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfig {
    @Bean(name = "asyncExecutor")
    public TaskExecutor asyncExecutor() {
        return new SimpleAsyncTaskExecutor("async-");
    }
}
