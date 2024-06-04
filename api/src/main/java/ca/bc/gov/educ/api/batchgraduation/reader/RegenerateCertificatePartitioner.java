package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.*;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

public class RegenerateCertificatePartitioner extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegenerateCertificatePartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Autowired
    JsonTransformer jsonTransformer;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        createBatchJobHistory();
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString(SEARCH_REQUEST);
        CertificateRegenerationRequest certificateRegenerationRequest = (CertificateRegenerationRequest)jsonTransformer.unmarshall(searchRequest, CertificateRegenerationRequest.class);
        if (certificateRegenerationRequest == null || certificateRegenerationRequest.runForAll()) {
            Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredData();
            DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
            if(parallelDTO != null) {
                credentialList.addAll(parallelDTO.certificateList());
            }
        } else if (certificateRegenerationRequest.getPens() != null && !certificateRegenerationRequest.getPens().isEmpty()) {
            certificateRegenerationRequest.getPens().forEach(p -> {
                StudentCredentialDistribution scd = new StudentCredentialDistribution();
                scd.setPen(p);
                scd.setStudentID(getStudentIDByPen(p));
                if (scd.getStudentID() != null)
                    credentialList.add(scd);
            });
        } else {
            credentialList.addAll(getStudentsForUserReqRun(certificateRegenerationRequest));
        }

        if(!credentialList.isEmpty()) {
            createTotalSummaryDTO("regenCertSummaryDTO");
            updateBatchJobHistory(createBatchJobHistory(), (long) credentialList.size());
            int partitionSize = credentialList.size()/gridSize + 1;
            List<List<StudentCredentialDistribution>> partitions = new LinkedList<>();
            for (int i = 0; i < credentialList.size(); i += partitionSize) {
                partitions.add(credentialList.subList(i, Math.min(i + partitionSize, credentialList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
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
        LOGGER.info("No Certificates are found to process them with null distribution date");
        return new HashMap<>();
    }

    // retrieve students based on the search criteria requested by user
    private List<StudentCredentialDistribution> getStudentsForUserReqRun(CertificateRegenerationRequest certificateRegenerationRequest) {
        if(certificateRegenerationRequest != null && "Y".equalsIgnoreCase(certificateRegenerationRequest.getRunMode())) {
            return restUtils.getStudentsForUserReqDisRunWithNullDistributionDate("OC", certificateRegenerationRequest);
        } else {
            return new ArrayList<>();
        }
    }

    private UUID getStudentIDByPen(String pen) {
        try {
            String accessToken = restUtils.fetchAccessToken();
            return restUtils.getStudentIDByPen(pen, accessToken);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
