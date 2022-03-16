package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradAlgorithmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public class RegGradAlgPartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegGradAlgPartitionHandlerCreator.class);

    @Autowired
    GradAlgorithmService gradAlgorithmService;

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['data']}")
    List<GraduationStudentRecord> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.jobId}")
    Long batchId;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Process partitioned data in parallel asynchronously
        partitionData.stream().forEach(d -> {
            if (summaryDTO.getProcessedCount() % 10 == 0) {
                summaryDTO.setAccessToken(fetchAccessToken());
            }
            summaryDTO.setBatchId(batchId);
            LOGGER.info("{} processing partitionData = {}",Thread.currentThread().getName(), d.getProgram());
            try {
                gradAlgorithmService.processStudent(d, summaryDTO);
            }catch (Exception e) {
                LOGGER.info("Student Errored Out");
            }
        });
        System.out.println(Thread.currentThread().getName() + " summary processed count = " + summaryDTO.getProcessedCount());

        // Aggregate summary
        aggregate(contribution);
        return RepeatStatus.FINISHED;
    }

    private void aggregate(StepContribution contribution) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get("regGradAlgSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new AlgorithmSummaryDTO();
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("regGradAlgSummaryDTO", totalSummaryDTO);
        }
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().addAll(summaryDTO.getErrors());
        Map<String,Long> programMap = summaryDTO.getProgramCountMap();

        AlgorithmSummaryDTO finalTotalSummaryDTO = totalSummaryDTO;
        programMap.forEach((k, v)-> {
            if(finalTotalSummaryDTO.getProgramCountMap().containsKey(k)) {
                finalTotalSummaryDTO.getProgramCountMap().put(k, finalTotalSummaryDTO.getProgramCountMap().get(k)+v);
            }else {
                finalTotalSummaryDTO.getProgramCountMap().put(k,v);
            }
        });
    }

    private String fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            return res.getAccess_token();
        }
        return null;
    }
}