package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import io.netty.channel.ConnectTimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class RESTServicePostTest {

    @Autowired
    @InjectMocks
    private RESTService restService;

    @MockBean
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @MockBean
    private WebClient.RequestBodySpec requestBodyMock;
    @MockBean
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @MockBean
    private WebClient.ResponseSpec responseMock;

    @MockBean
    @Qualifier("webClient")
    WebClient webClient;

    @MockBean
    @Qualifier("batchClient")
    WebClient batchWebClient;

    @Mock
    private ClientRegistrationRepository clientRegistrationRepositoryMock;

    @Mock
    private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepositoryMock;

    private static final byte[] TEST_BYTES = "How much wood would a woodchuck chuck if a woodchuck could chuck wood?".getBytes();
    private static final String TEST_BODY = "{test:test}";
    private static final String ACCESS_TOKEN = "123";
    private static final String TEST_URL = "https://fake.url.com";

    @Before
    public void setUp(){
        Mockito.reset(webClient, batchWebClient, responseMock, requestHeadersMock, requestBodyMock, requestBodyUriMock);
        ThreadLocalStateUtil.clear();
        when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.batchWebClient.post()).thenReturn(this.requestBodyUriMock);
        when(this.requestBodyUriMock.uri(any(String.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.headers(any(Consumer.class))).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.contentType(any())).thenReturn(this.requestBodyMock);
        when(this.requestBodyMock.body(any(BodyInserter.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(byte[].class)).thenReturn(Mono.just(TEST_BYTES));
    }

    @Test
    public void testPost_GivenProperData_Expect200Response(){
        ThreadLocalStateUtil.setCorrelationID("test-correlation-id");
        ThreadLocalStateUtil.setCurrentUser("test-user");
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        byte[] response = this.restService.post(TEST_URL, TEST_BODY, byte[].class, ACCESS_TOKEN);
        Assert.assertArrayEquals(TEST_BYTES, response);
    }

    @Test
    public void testPostOverride_GivenProperData_Expect200Response(){
        ThreadLocalStateUtil.setCorrelationID("test-correlation-id");
        ThreadLocalStateUtil.setCurrentUser("test-user");
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
        byte[] response = this.restService.post(TEST_URL, TEST_BODY, byte[].class);
        Assert.assertArrayEquals(TEST_BYTES, response);
    }

    @Test(expected = ServiceException.class)
    public void testPost_Given4xxErrorFromService_ExpectServiceError() {
        when(this.responseMock.onStatus(any(), any())).thenThrow(new ServiceException());
        this.restService.post(TEST_URL, TEST_BODY, byte[].class, ACCESS_TOKEN);
    }

    @Test(expected = ServiceException.class)
    public void testPostOverride_Given4xxErrorFromService_ExpectServiceError() {
        when(this.responseMock.onStatus(any(), any())).thenThrow(new ServiceException());
        this.restService.post(TEST_URL, TEST_BODY, byte[].class);
    }

    @Test(expected = ServiceException.class)
    public void testPost_Given5xxErrorFromService_ExpectConnectionError(){
        when(requestBodyUriMock.uri(TEST_URL)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        when(responseMock.bodyToMono(byte[].class)).thenReturn(Mono.error(new ConnectTimeoutException("Connection closed")));
        this.restService.post(TEST_URL, TEST_BODY, byte[].class);
    }

    @Test(expected = ServiceException.class)
    public void testPost_Given5xxErrorFromService_ExpectWebClientRequestError(){
        when(requestBodyUriMock.uri(TEST_URL)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        Throwable cause = new RuntimeException("Simulated cause");
        when(responseMock.bodyToMono(byte[].class)).thenReturn(Mono.error(new WebClientRequestException(cause, HttpMethod.POST, null, new HttpHeaders())));
        this.restService.post(TEST_URL, TEST_BODY, byte[].class);
    }

    @Test(expected = ServiceException.class)
    public void testPostWithToken_Given5xxErrorFromService_ExpectWebClientRequestError(){
        when(requestBodyUriMock.uri(TEST_URL)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        Throwable cause = new RuntimeException("Simulated cause");
        when(responseMock.bodyToMono(byte[].class)).thenReturn(Mono.error(new WebClientRequestException(cause, HttpMethod.POST, null, new HttpHeaders())));
        this.restService.post(TEST_URL, TEST_BODY, byte[].class, "ABC");
    }
}
