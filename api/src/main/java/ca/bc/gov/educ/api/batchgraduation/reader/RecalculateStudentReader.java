package ca.bc.gov.educ.api.batchgraduation.reader;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;

public class RecalculateStudentReader implements ItemReader<GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecalculateStudentReader.class);

    private final String apiUrl;
    
    private final RestTemplate restTemplate;

    private int nxtStudentForProcessing;
    private List<GraduationStatus> studentList;
    
    @Value("${authorization.user}")
   	private String uName;

   	@Value("${authorization.password}")
   	private String pass;
   	
   	@Value(EducGradBatchGraduationApiConstants.ENDPOINT_GET_TOKEN_URL)
    private String getToken;

    public RecalculateStudentReader(String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        nxtStudentForProcessing = 0;
    }

    @Override
    public GraduationStatus read() throws Exception {
        LOGGER.info("Reading the information of the next student");

        if (studentDataIsNotInitialized()) {
        	studentList = fetchStudentDataFromAPI();
        }

        GraduationStatus nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("Found student: **** PEN: **** {}", nextStudent.getPen().substring(5));
            nxtStudentForProcessing++;
        }
        else {
        	nxtStudentForProcessing = 0;
            studentList = null;
        }
        return nextStudent;
    }

    private boolean studentDataIsNotInitialized() {
        return this.studentList == null;
    }

    private List<GraduationStatus> fetchStudentDataFromAPI() {
        LOGGER.info("Fetching Student List that need Processing");
        HttpHeaders httpHeadersKC = EducGradBatchGraduationApiUtils.getHeaders(uName,pass);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("grant_type", "client_credentials");
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, httpHeadersKC);
		ResponseObj res = restTemplate.exchange(getToken, HttpMethod.POST,
				request, ResponseObj.class).getBody();
        HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(res.getAccess_token());			
		List<GraduationStatus> gradStudentList = restTemplate.exchange(apiUrl, HttpMethod.GET,
					new HttpEntity<>(httpHeaders), new ParameterizedTypeReference<List<GraduationStatus>>() {}).getBody();
		gradStudentList.forEach(gS-> {
			gS.setAccess_token(res.getAccess_token());
		});
		return gradStudentList;
    }
}
