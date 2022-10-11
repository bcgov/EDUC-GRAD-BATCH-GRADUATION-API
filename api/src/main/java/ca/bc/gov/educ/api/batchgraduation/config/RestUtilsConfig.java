package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.model.ResponseObjCache;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestUtilsConfig {

    @Autowired
    EducGradBatchGraduationApiConstants constants;

    @Bean
    public ResponseObjCache createResponseObjCache() {
        return new ResponseObjCache(constants.getTokenExpiryOffset());
    }

}
