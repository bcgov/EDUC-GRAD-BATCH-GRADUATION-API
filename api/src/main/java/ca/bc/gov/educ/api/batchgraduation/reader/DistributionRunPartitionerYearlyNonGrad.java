package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.*;

public class DistributionRunPartitionerYearlyNonGrad extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunPartitionerYearlyNonGrad.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredDataNonGradYearly(accessToken);
        DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        if(parallelDTO != null) {
            credentialList.addAll(parallelDTO.transcriptList());
            credentialList.addAll(parallelDTO.certificateList());
        }
        if(!credentialList.isEmpty()) {
            int partitionSize = credentialList.size()/gridSize + 1;
            List<List<StudentCredentialDistribution>> partitions = new LinkedList<>();
            for (int i = 0; i < credentialList.size(); i += partitionSize) {
                partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                List<StudentCredentialDistribution> data = partitions.get(i);
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
}
