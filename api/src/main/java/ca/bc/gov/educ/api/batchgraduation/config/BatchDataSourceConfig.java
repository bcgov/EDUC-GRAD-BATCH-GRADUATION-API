package ca.bc.gov.educ.api.batchgraduation.config;

import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

//@Configuration
//@Profile("!test")
//@Component
public class BatchDataSourceConfig extends DefaultBatchConfigurer {

    @Autowired
    public BatchDataSourceConfig(@Qualifier("batchDataSource") DataSource batchDataSource) {
        super(batchDataSource);
    }
}
