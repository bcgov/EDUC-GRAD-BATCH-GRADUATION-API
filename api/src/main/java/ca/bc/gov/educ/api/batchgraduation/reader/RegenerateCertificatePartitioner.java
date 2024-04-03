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
import java.util.stream.Collectors;

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
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        JobParameters jobParameters = context.getJobParameters();
        String searchRequest = jobParameters.getString(SEARCH_REQUEST);
        CertificateRegenerationRequest certificateRegenerationRequest = (CertificateRegenerationRequest)jsonTransformer.unmarshall(searchRequest, CertificateRegenerationRequest.class);
        if (certificateRegenerationRequest.runForAll()) {
            Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredData(accessToken);
            DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
            if(parallelDTO != null) {
                credentialList.addAll(parallelDTO.certificateList());
            }
        } else {
            credentialList.addAll(getStudentsForUserReqRun(certificateRegenerationRequest, accessToken));
        }

        Set<UUID> studentSet = new HashSet<>();
        if(!credentialList.isEmpty()) {
            studentSet = credentialList.stream().map(StudentCredentialDistribution::getStudentID).collect(Collectors.toSet());
        }

        List<UUID> studentList = new ArrayList<>(studentSet);
        if(!studentList.isEmpty()) {
            createTotalSummaryDTO("regenCertSummaryDTO");
            updateBatchJobHistory(createBatchJobHistory(), (long) studentList.size());
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
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",studentList.size(),map.size());
            return map;
        }
        LOGGER.info("No Certificates are found to process them with null distribution date");
        return new HashMap<>();
    }

    // retrieve students based on the search criteria requested by user
    private List<StudentCredentialDistribution> getStudentsForUserReqRun(CertificateRegenerationRequest certificateRegenerationRequest, String accessToken) {
        if(certificateRegenerationRequest != null && "Y".equalsIgnoreCase(certificateRegenerationRequest.getRunMode())) {
            return restUtils.getStudentsForUserReqDisRun("OC", certificateRegenerationRequest, accessToken);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
