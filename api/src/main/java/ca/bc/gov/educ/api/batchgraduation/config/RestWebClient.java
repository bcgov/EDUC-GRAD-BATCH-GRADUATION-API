package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.LogHelper;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@Profile("!test")
public class RestWebClient {

    private final HttpClient httpClient;
    LogHelper logHelper;
    EducGradBatchGraduationApiConstants constants;

    public RestWebClient() {
        this.httpClient = HttpClient.create(ConnectionProvider.create("batch-api")).compress(true)
                .resolver(spec -> spec.queryTimeout(Duration.ofSeconds(5)).trace("DNS", LogLevel.TRACE));
        this.httpClient.warmup().block();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .filter(setRequestHeaders())
                .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(300 * 1024 * 1024))  // 300 MB
                      .build())
                .filter(this.log())
                .build();
    }

    @Bean("batchClient")
    public WebClient getBatchWebClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        filter.setDefaultClientRegistrationId("batch-client");
        return WebClient.builder()
                .filter(setRequestHeaders())
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                .maxInMemorySize(300 * 1024 * 1024)) // 300 MB
                        .build())
                .apply(filter.oauth2Configuration())
                .filter(this.log())
                .build();
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService clientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();
        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
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

    private ExchangeFilterFunction log() {
        return (clientRequest, next) -> next
                .exchange(clientRequest)
                .doOnNext((clientResponse -> logHelper.logClientHttpReqResponseDetails(
                        clientRequest.method(),
                        clientRequest.url().toString(),
                        clientResponse.statusCode().value(),
                        clientRequest.headers().get(EducGradBatchGraduationApiConstants.CORRELATION_ID),
                        clientRequest.headers().get(EducGradBatchGraduationApiConstants.REQUEST_SOURCE),
                        constants.isSplunkLogHelperEnabled())
                ));
    }
}
