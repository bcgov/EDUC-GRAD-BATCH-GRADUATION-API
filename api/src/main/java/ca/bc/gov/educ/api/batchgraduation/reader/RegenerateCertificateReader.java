package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RegenerateCertificateReader extends BaseReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegenerateCertificateReader.class);

    @Override
    public UUID read() throws Exception {
        fetchAccessToken();
        summaryDTO.setReadCount(studentList.size());

        UUID nextStudent = null;
        
        if (nxtStudentForProcessing < studentList.size()) {
            nextStudent = studentList.get(nxtStudentForProcessing);
            LOGGER.info("StudID:{} - {} of {}", nextStudent, nxtStudentForProcessing + 1, summaryDTO.getReadCount());
            nxtStudentForProcessing++;
        } else {
            aggregate("regenCertSummaryDTO");
        }
        return nextStudent;
    }

    public void aggregate(String summaryContextName) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new AlgorithmSummaryDTO();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().putAll(summaryDTO.getErrors());
    }
}
