package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@Profile("test")
public class RestWebClient {
    @Bean("webClient")
    public WebClient webClient() {
        return WebClient.builder()
                .filter(setRequestHeaders())
                .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024))  // 100 MB
                .build()).build();
    }

    @Bean("batchClient")
    public WebClient getBatchWebClient() {
        return WebClient.builder()
                .filter(setRequestHeaders())
                .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024))  // 100 MB
                .build()).build();
    }

    private ExchangeFilterFunction setRequestHeaders() {
        return (clientRequest, next) -> {
            ClientRequest modifiedRequest = ClientRequest.from(clientRequest)
                    .header(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID())
                    .header(EducGradBatchGraduationApiConstants.USER_NAME, ThreadLocalStateUtil.getCurrentUser())
                    .header(EducGradBatchGraduationApiConstants.REQUEST_SOURCE, EducGradBatchGraduationApiConstants.API_NAME)
                    .build();
            return next.exchange(modifiedRequest);
        };
    }
}
