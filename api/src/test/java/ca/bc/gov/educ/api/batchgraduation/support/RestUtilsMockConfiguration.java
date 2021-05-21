package ca.bc.gov.educ.api.batchgraduation.support;

import ca.bc.gov.educ.api.batchgraduation.util.RestUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("testRestUtils")
@Configuration
public class RestUtilsMockConfiguration {

    @Bean
    @Primary
    public RestUtils restUtils() { return Mockito.mock(RestUtils.class); }

}
