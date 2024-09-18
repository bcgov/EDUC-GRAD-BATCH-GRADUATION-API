package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObjCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static ca.bc.gov.educ.api.batchgraduation.rest.RESTService.ERROR_MESSAGE1;
import static ca.bc.gov.educ.api.batchgraduation.rest.RESTService.ERROR_MESSAGE2;

@Component
public class TokenUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenUtils.class);

    private ResponseObjCache responseObjCache;
    private final EducGradBatchGraduationApiConstants constants;
    private final WebClient webClient;

    @Autowired
    public TokenUtils(final EducGradBatchGraduationApiConstants constants, final WebClient webClient, final ResponseObjCache responseObjCache) {
        this.constants = constants;
        this.webClient = webClient;
        this.responseObjCache = responseObjCache;
    }

    public String fetchAccessToken() {
        return this.getTokenResponseObject().getAccess_token();
    }

    public ResponseObj getTokenResponseObject() {
        if(responseObjCache.isExpired()){
            responseObjCache.setResponseObj(getResponseObj());
        }
        return responseObjCache.getResponseObj();
    }

    public ResponseObj getResponseObj() {
        LOGGER.debug("Fetch token");
        HttpHeaders httpHeadersKC = EducGradBatchGraduationApiUtils.getHeaders(
                constants.getUserName(), constants.getPassword());
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> Mono.error(new ServiceException(getErrorMessage(constants.getTokenUrl(), ERROR_MESSAGE1), clientResponse.statusCode().value())))
                .bodyToMono(ResponseObj.class)
                .retryWhen(reactor.util.retry.Retry.backoff(constants.getTokenRetryMaxAttempts(), Duration.ofSeconds(constants.getTokenRetryWaitDurationSeconds()))
                        .filter(ServiceException.class::isInstance)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new ServiceException(getErrorMessage(constants.getTokenUrl(), ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                        }))
                .block();
    }

    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }
}
