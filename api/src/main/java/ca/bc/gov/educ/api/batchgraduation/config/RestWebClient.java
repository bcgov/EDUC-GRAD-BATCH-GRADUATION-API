package ca.bc.gov.educ.api.batchgraduation.config;

import io.netty.handler.logging.LogLevel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
@Profile("!test")
public class RestWebClient {
    private final HttpClient httpClient;

    public RestWebClient() {
        this.httpClient = HttpClient.create(ConnectionProvider.create("batch-api")).compress(true)
                .resolver(spec -> spec.queryTimeout(Duration.ofSeconds(5)).trace("DNS", LogLevel.TRACE));
        this.httpClient.warmup().block();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(300 * 1024 * 1024))  // 100 MB
                      .build()).build();
    }
}
