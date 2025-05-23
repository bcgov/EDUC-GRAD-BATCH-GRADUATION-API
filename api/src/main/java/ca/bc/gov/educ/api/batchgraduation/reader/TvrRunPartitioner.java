package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.RunTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class TvrRunPartitioner extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvrRunPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    public TvrRunPartitioner() {
        super();
    }

    @Override
    public JobExecution getJobExecution() {
        return jobExecution;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        initializeRunType();
        BatchGradAlgorithmJobHistoryEntity jobHistory = createBatchJobHistory();
        JobParameters jobParameters = jobExecution.getJobParameters();
        String username = jobParameters.getString(RUN_BY);
        List<UUID> studentList;
        if (runType == RunTypeEnum.NORMAL_JOB_PROCESS) {
            studentList = restUtils.getStudentsForProjectedAlgorithm();
        } else {
            studentList = getInputDataFromPreviousJob();
        }
        createTotalSummaryDTO("tvrRunSummaryDTO");
        updateBatchJobHistory(jobHistory, Long.valueOf(studentList.size()));
        if(!studentList.isEmpty()) {
            if (runType == RunTypeEnum.NORMAL_JOB_PROCESS) {
                saveInputData(studentList);
            }
            int partitionSize = studentList.size()/gridSize + 1;
            List<List<UUID>> partitions = new LinkedList<>();
            for (int i = 0; i < studentList.size(); i += partitionSize) {
                partitions.add(studentList.subList(i, Math.min(i + partitionSize, studentList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
                List<UUID> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                executionContext.put("runBy", username);
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
