package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import io.netty.channel.ConnectTimeoutException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class RESTServiceGetTest {

    @Autowired
    private RESTService restService;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
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

    private static final String TEST_URL_200 = "https://httpstat.us/200";
    private static final String TEST_URL_403 = "https://httpstat.us/403";
    private static final String TEST_URL_503 = "https://httpstat.us/503";
    private static final String OK_RESPONSE = "200 OK";

    @Before
    public void setUp(){
        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.batchWebClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
    }

    @Test
    public void testGet_GivenProperData_Expect200Response(){
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just(OK_RESPONSE));
        String response = this.restService.get(TEST_URL_200, String.class, "1234");
        Assert.assertEquals("200 OK", response);
    }

    @Test
    public void testGetOverride_GivenProperData_Expect200Response(){
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just(OK_RESPONSE));
        String response = this.restService.get(TEST_URL_200, String.class);
        Assert.assertEquals(OK_RESPONSE, response);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.get(TEST_URL_503, String.class, "1234");
    }

    @Test(expected = ServiceException.class)
    public void testGetOverride_Given5xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.get(TEST_URL_503, String.class);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given4xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.get(TEST_URL_403, String.class, "1234");
    }

    @Test(expected = ServiceException.class)
    public void testGetOverride_Given4xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.get(TEST_URL_403, String.class);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectConnectionError(){
        when(requestBodyUriMock.uri(TEST_URL_503)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        Throwable cause = new RuntimeException("Simulated cause");
        when(responseMock.bodyToMono(String.class)).thenReturn(Mono.error(new ConnectTimeoutException("Connection closed")));
        restService.get(TEST_URL_503, String.class);
    }

    @Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectWebClientRequestError(){
        when(requestBodyUriMock.uri(TEST_URL_503)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        Throwable cause = new RuntimeException("Simulated cause");
        when(responseMock.bodyToMono(String.class)).thenReturn(Mono.error(new WebClientRequestException(cause, HttpMethod.GET, null, new HttpHeaders())));
        restService.get(TEST_URL_503, String.class);
    }

    @Test(expected = ServiceException.class)
    public void testGetWithToken_Given5xxErrorFromService_ExpectWebClientRequestError(){
        when(requestBodyUriMock.uri(TEST_URL_503)).thenReturn(requestBodyMock);
        when(requestBodyMock.retrieve()).thenReturn(responseMock);

        Throwable cause = new RuntimeException("Simulated cause");
        when(responseMock.bodyToMono(String.class)).thenReturn(Mono.error(new WebClientRequestException(cause, HttpMethod.GET, null, new HttpHeaders())));
        restService.get(TEST_URL_503, String.class, "ABC");
    }


}
