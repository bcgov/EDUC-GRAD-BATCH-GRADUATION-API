package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.rest.RESTGenerics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class RestGenericsGetTest {

    @Autowired
    private RESTGenerics restGenerics;

    //@Test
    public void testGet_GivenProperData_Expect200Response(){
        String response;
        response = this.restGenerics.get("https://httpstat.us/200", String.class, "1234");
        Assert.assertEquals("200 OK", response);
    }

    //@Test(expected = ServiceException.class)
    public void testGet_Given5xxErrorFromService_ExpectServiceError(){
        this.restGenerics.get("https://httpstat.us/503", String.class, "1234");
    }

    //@Test(expected = ServiceException.class)
    public void testGet_Given4xxErrorFromService_ExpectServiceError(){
        this.restGenerics.get("https://httpstat.us/403", String.class, "1234");
    }

    @Test
    public void testDoNothing() {
        Assert.assertTrue(true);
    }

}
