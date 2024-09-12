package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class RESTServiceDeleteTest {

    @Autowired
    @InjectMocks
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
        when(this.batchWebClient.delete()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(any(String.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.onStatus(any(), any())).thenReturn(this.responseMock);
    }

    @Test
    public void testDelete_GivenProperData_Expect200Response(){
        when(this.responseMock.bodyToMono(String.class)).thenReturn(Mono.just(OK_RESPONSE));
        String response = this.restService.delete(TEST_URL_200, String.class);
        Assert.assertEquals("200 OK", response);
    }

    @Test(expected = ServiceException.class)
    public void testDelete_Given5xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.delete(TEST_URL_503, String.class);
    }

    @Test(expected = ServiceException.class)
    public void testDelete_Given4xxErrorFromService_ExpectServiceError(){
        when(this.responseMock.bodyToMono(ServiceException.class)).thenReturn(Mono.just(new ServiceException()));
        this.restService.delete(TEST_URL_403, String.class);
    }

}
