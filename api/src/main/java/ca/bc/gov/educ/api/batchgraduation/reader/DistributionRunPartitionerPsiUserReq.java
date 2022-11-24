package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialRequest;
import ca.bc.gov.educ.api.batchgraduation.model.PsiDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class DistributionRunPartitionerPsiUserReq extends BaseDistributionPartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunPartitionerPsiUserReq.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Autowired
    GraduationReportService graduationReportService;

    @Override
    public JobExecution getJobExecution() {
        return context;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        BatchGradAlgorithmJobHistoryEntity jobHistory = createBatchJobHistory();
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString("searchRequest");
        String transmissionType = jobParameters.getString("transmissionType");
        PsiCredentialRequest req = null;
        try {
            req = new ObjectMapper().readValue(searchRequest, PsiCredentialRequest.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        ResponseObj responseObj = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (responseObj != null) {
            accessToken = responseObj.getAccess_token();
        }

        List<PsiCredentialDistribution> credentialList = getRecordsForPSIUserReqDisRun(req,transmissionType,accessToken);
        updateBatchJobHistory(jobHistory, Long.valueOf(credentialList.size()));
        if(!credentialList.isEmpty()) {
            int partitionSize = credentialList.size()/gridSize + 1;
            List<List<PsiCredentialDistribution>> partitions = new LinkedList<>();
            for (int i = 0; i < credentialList.size(); i += partitionSize) {
                partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                PsiDistributionSummaryDTO summaryDTO = new PsiDistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                List<PsiCredentialDistribution> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",credentialList.size(),map.size());
            return map;
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    private List<PsiCredentialDistribution> getRecordsForPSIUserReqDisRun(PsiCredentialRequest req, String transmissionType, String accessToken) {
        if(req != null) {
            List<String> psiCodeList = req.getPsiCodes();
            String psiCodes = String.join(",", psiCodeList);
            String psiYear = req.getPsiYear();

            return graduationReportService.getPsiStudentsForRun(transmissionType,psiCodes,psiYear,accessToken);
        }
        return new ArrayList<>();
    }
}
