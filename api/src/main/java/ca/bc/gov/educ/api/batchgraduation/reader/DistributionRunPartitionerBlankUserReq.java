package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class DistributionRunPartitionerBlankUserReq extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunPartitionerBlankUserReq.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString("searchRequest");
        String credentialType = jobParameters.getString("credentialType");
        BlankCredentialRequest req = null;
        try {
            req = new ObjectMapper().readValue(searchRequest, BlankCredentialRequest.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        List<BlankCredentialDistribution> credentialList = getRecordsForBlankUserReqDisRun(req);
        if(!credentialList.isEmpty()) {
            int partitionSize = credentialList.size()/gridSize + 1;
            List<List<BlankCredentialDistribution>> partitions = new LinkedList<>();
            for (int i = 0; i < credentialList.size(); i += partitionSize) {
                partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                List<BlankCredentialDistribution> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                summaryDTO.setCredentialType(credentialType);
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

    private List<BlankCredentialDistribution> getRecordsForBlankUserReqDisRun(BlankCredentialRequest req) {
        if(req != null) {
            List<String> schoolList = req.getSchoolOfRecords();
            List<BlankCredentialDistribution> blankList = new ArrayList<>();
            schoolList.forEach(sch -> {
                for (String ctc : req.getCredentialTypeCode()) {
                    BlankCredentialDistribution bcd = new BlankCredentialDistribution();
                    bcd.setQuantity(req.getQuantity());
                    bcd.setSchoolOfRecord(sch);
                    bcd.setCredentialTypeCode(ctc);
                    blankList.add(bcd);
                }
            });
            return blankList;
        }
        return new ArrayList<>();
    }
}
