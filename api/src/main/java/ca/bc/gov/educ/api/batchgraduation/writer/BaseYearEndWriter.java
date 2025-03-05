package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.listener.SupportListener;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BaseYearEndWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseYearEndWriter.class);

    @Autowired
    RestUtils restUtils;
    @Autowired
    DistributionService distributionService;
    @Autowired
    SupportListener supportListener;
    @Autowired
    JsonTransformer jsonTransformer;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.id}")
    Long jobExecutionId;

    @Value("#{stepExecution.jobExecution.status}")
    BatchStatus status;

    @Value("#{stepExecution.jobExecution.jobParameters}")
    JobParameters jobParameters;

    @Value("#{stepExecution.jobExecution.startTime}")
    LocalDateTime startTime;

    @Value("#{stepExecution.jobExecution.endTime}")
    LocalDateTime endTime;

    protected void processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<UUID, DistributionPrintRequest> mapDist, String activityCode, String accessToken) {
        List<UUID> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolId).distinct().collect(Collectors.toList());
        uniqueSchoolList.forEach(usl->{
            List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolId().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
            List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->scd.getSchoolId().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen())).collect(Collectors.toList());
            supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);
            schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
        });
        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(mapDist).activityCode(activityCode).build();
        distributionRequest.setSchools(summaryDTO.getSchools());
        restUtils.mergeAndUpload(batchId, distributionRequest,activityCode,"N");
    }

    protected void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, UUID usl, Map<UUID,DistributionPrintRequest> mapDist) {
        if(!studentList.isEmpty()) {
            SchoolDistributionRequest tpReq = new SchoolDistributionRequest();
            tpReq.setBatchId(batchId);
            tpReq.setPsId(usl +" " +batchId);
            tpReq.setCount(studentList.size());
            tpReq.setStudentList(studentList);
            if(mapDist.get(usl) != null) {
                DistributionPrintRequest dist = mapDist.get(usl);
                dist.setSchoolDistributionRequest(tpReq);
                mapDist.put(usl,dist);
            } else {
                DistributionPrintRequest dist = new DistributionPrintRequest();
                dist.setSchoolDistributionRequest(tpReq);
                mapDist.put(usl,dist);
            }
        }
    }
}
