package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.EducGradBatchGraduationApplication;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class RestUtilsPostTest {

    @Autowired
    private RestUtils restUtils;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public WebClient webClient() {
            return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(100 * 1024 * 1024))  // 100 MB
                    .build()).build();
        }
    }

    @Test
    public void testGet_GivenProperData_Expect200Response(){
        String response;
        response = this.restUtils.get("https://httpstat.us/200", String.class, "1234");
        Assert.assertEquals("200 OK", response);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectServiceError(){
        this.restUtils.get("https://httpstat.us/503", String.class, "1234");
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given4xxErrorFromService_ExpectServiceError(){
        this.restUtils.get("https://httpstat.us/403", String.class, "1234");
    }

}
