package ca.bc.gov.educ.api.batchgraduation.writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.util.GradDataStore;

public class BatchPerformanceWriter implements ItemWriter<GraduationStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchPerformanceWriter.class);
    
    @Autowired
    private GradDataStore gradDataStore;
    
    @Override
    public void write(List<? extends GraduationStatus> list) throws Exception {
        LOGGER.info("Recording Algorithm Processed Data");
        GraduationStatus gradStatus = list.get(0);
        gradDataStore.addProgram(gradStatus.getProgram());
        gradDataStore.addProcessedItem(gradStatus);
    }
}
