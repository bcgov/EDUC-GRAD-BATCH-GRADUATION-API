package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.model.ResponseObjCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestUtilsConfig {

    @Bean
    public ResponseObjCache createResponseObjCache(){
        return new ResponseObjCache();
    }

}
