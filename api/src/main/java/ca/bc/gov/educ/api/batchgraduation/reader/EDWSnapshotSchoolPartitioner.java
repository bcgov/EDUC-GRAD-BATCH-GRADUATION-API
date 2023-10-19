package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSchoolSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.SnapshotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

public class EDWSnapshotSchoolPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(EDWSnapshotSchoolPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        long startTime = System.currentTimeMillis();
        logger.debug("Retrieve schools for EDW Snapshot");
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString(SEARCH_REQUEST);
        SnapshotRequest req = (SnapshotRequest) jsonTransformer.unmarshall(searchRequest, SnapshotRequest.class);
        List<String> schools;
        if (req.getSchoolOfRecords() != null && !req.getSchoolOfRecords().isEmpty()) {
            schools = req.getSchoolOfRecords();
        } else {
            schools = restUtils.getEDWSnapshotSchools(req.getGradYear(), accessToken);
        }
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} schools in {} sec", schools.size(), diff);
        if(!schools.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) schools.size());
            int partitionSize = schools.size()/gridSize + 1;
            List<List<String>> partitions = new LinkedList<>();
            for (int i = 0; i < schools.size(); i += partitionSize) {
                partitions.add(schools.subList(i, Math.min(i + partitionSize, schools.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                EdwSnapshotSchoolSummaryDTO summaryDTO = new EdwSnapshotSchoolSummaryDTO();
                summaryDTO.setGradYear(req.getGradYear());
                List<String> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            logger.info("Found {} in total running on {} partitions",schools.size(),map.size());
            return map;
        }
        logger.info("No Schools Found from Snapshot for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
