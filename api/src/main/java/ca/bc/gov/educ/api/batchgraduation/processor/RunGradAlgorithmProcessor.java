package ca.bc.gov.educ.api.batchgraduation.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;

public class RunGradAlgorithmProcessor implements ItemProcessor<GraduationStatus,GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunGradAlgorithmProcessor.class);

    @Autowired
    RestTemplate restTemplate;
    
    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_RUN_GRADUATION_API_URL)
    private String graduateStudent;
    
	@Override
	public GraduationStatus process(GraduationStatus item) throws Exception {
		LOGGER.info(" Processing  "+item.getPen());
		HttpHeaders httpHeaders = EducGradBatchGraduationApiUtils.getHeaders(item.getAccess_token());
		try {
		 GraduationStatus graduationDataStatus = restTemplate.exchange(String.format(graduateStudent,item.getPen()), HttpMethod.GET,
				new HttpEntity<>(httpHeaders), GraduationStatus.class).getBody();
		 return graduationDataStatus;
		}catch(Exception e) {
			return item;
		}
		
	}

    
}
