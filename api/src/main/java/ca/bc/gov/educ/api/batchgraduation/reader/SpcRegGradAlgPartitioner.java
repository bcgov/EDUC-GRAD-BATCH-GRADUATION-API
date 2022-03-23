package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class SpcRegGradAlgPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpcRegGradAlgPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    public SpcRegGradAlgPartitioner() {}


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString("searchRequest");
        StudentSearchRequest req = null;
        try {
            req = new ObjectMapper().readValue(searchRequest, StudentSearchRequest.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        List<GraduationStudentRecord> studentList = restUtils.getStudentsForSpecialGradRun(req,accessToken);

        if(!studentList.isEmpty()) {
            int partitionSize = studentList.size()/gridSize + 1;
            List<List<GraduationStudentRecord>> partitions = new LinkedList<>();
            for (int i = 0; i < studentList.size(); i += partitionSize) {
                partitions.add(studentList.subList(i, Math.min(i + partitionSize, studentList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
                List<GraduationStudentRecord> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",studentList.size(),map.size());
            return map;
        }
        LOGGER.info("No Students Found for Processing");
        return new HashMap<>();
    }
}
