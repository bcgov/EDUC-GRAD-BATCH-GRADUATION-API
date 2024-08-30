package ca.bc.gov.educ.api.batchgraduation.rest;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class RESTGenerics {

    public static final String ERROR_MESSAGE1 = "Service failed to process after max retries.";
    public static final String ERROR_MESSAGE2 = "5xx error.";

    final EducGradBatchGraduationApiConstants constants;

    private final WebClient webClient;

    private final WebClient batchWebClient;

    @Autowired
    public RESTGenerics(@Qualifier("batchClient") WebClient batchWebClient, WebClient webClient, final EducGradBatchGraduationApiConstants constants) {
        this.constants = constants;
        this.webClient = webClient;
        this.batchWebClient = batchWebClient;
    }

    /**
     * Generic GET call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T get(String url, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = this.webClient
                    .get()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .retrieve()
                    // if 5xx errors, throw Service error
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403 etc., so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    public <T> T get(String url, Class<T> clazz) {
        T obj;
        try {
            obj = this.batchWebClient
                    .get()
                    .uri(url)
                    .headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
                    .retrieve()
                    // if 5xx errors, throw Service error
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403 etc., so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    public <T> T get(String url, MultiValueMap<String, String> queryParams, Class<T> clazz) {
        T obj;
        try {
            obj = this.batchWebClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(url).queryParams(queryParams)
                            .build()
                    )
                    .headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
                    .retrieve()
                    // if 5xx errors, throw Service error
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403 etc., so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    /**
     * Generic POST call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param body the body you are requesting
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T post(String url, Object body, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = webClient.post()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    public <T> T post(String url, Object body, Class<T> clazz) {
        T obj;
        try {
            obj = batchWebClient.post()
                    .uri(url)
                    .headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    /**
     * Generic PUT call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param body the body you are requesting
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T put(String url, Object body, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = this.webClient.put()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    public <T> T put(String url, Object body, Class<T> clazz) {
        T obj;
        try {
            obj = this.webClient.put()
                    .uri(url)
                    .headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    public <T> T delete(String url, Class<T> boundClass, String accessToken) {
        T obj;
        try {
            obj = webClient.delete().uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .retrieve().bodyToMono(boundClass).block();
        } catch(Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(
                    url,
                    e.getLocalizedMessage()),
                    (e instanceof WebClientResponseException) ? ((WebClientResponseException) e).getStatusCode().value() : HttpStatus.SERVICE_UNAVAILABLE.value(),
                    e);
        }
        return obj;
    }

    protected String parseUrlParameters(String url, String... params) {
        List<String> l = Arrays.asList(params);
        l.replaceAll(t-> Objects.isNull(t) ? "" : t);
        return String.format(url, l.toArray());
    }

    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }
}
