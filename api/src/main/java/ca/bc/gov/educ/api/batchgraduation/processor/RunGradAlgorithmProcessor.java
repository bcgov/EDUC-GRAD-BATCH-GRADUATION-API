package ca.bc.gov.educ.api.batchgraduation.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmResponse;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;

public class RunGradAlgorithmProcessor implements ItemProcessor<GraduationStatus,GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunGradAlgorithmProcessor.class);

    @Autowired
    RestTemplate restTemplate;

	@Autowired
	EducGradBatchGraduationApiConstants constants;
    
	@Override
	public GraduationStatus process(GraduationStatus item) throws Exception {
		LOGGER.info(" Processing  **** PEN: ****" + item.getPen().substring(5));
		HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(item.getAccess_token());
		try {
		AlgorithmResponse algorithmResponse = restTemplate.exchange(String.format(constants.getGraduationApiUrl(),item.getStudentID()), HttpMethod.GET,
				new HttpEntity<>(httpHeaders), AlgorithmResponse.class).getBody();
		 return algorithmResponse.getGraduationStatus();
		}catch(Exception e) {
			return null;
		}
		
	}

    
}
