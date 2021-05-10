package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.service.DataConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DataConversionStudentReader implements ItemReader<ConvGradStudent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataConversionStudentReader.class);

    @Autowired
    private DataConversionService dataConversionService;

    private int nxtStudentForProcessing;
    private List<ConvGradStudent> studentList;


    public DataConversionStudentReader() {
        nxtStudentForProcessing = 0;
    }

    @Override
    public ConvGradStudent read() throws Exception {
        LOGGER.info("Reading the information of the next student");

        if (studentDataIsNotInitialized()) {
        	studentList = loadRawStudentData();
        }

        ConvGradStudent nextStudent = null;
        
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

    private List<ConvGradStudent> loadRawStudentData() throws Exception {
        LOGGER.info("Fetching Student List that need Processing");
      return dataConversionService.loadInitialRawGradStudentData(false);
    }
}
