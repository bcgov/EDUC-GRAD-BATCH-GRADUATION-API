package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.listener.SupportListener;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DistributionRunWriterYearlyNonGrad implements ItemWriter<List<StudentCredentialDistribution>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunWriterYearlyNonGrad.class);

    @Autowired
    RestUtils restUtils;

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
    Date startTime;

    @Value("#{stepExecution.jobExecution.endTime}")
    Date endTime;

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) throws Exception {
        if(!list.isEmpty()) {
            List<StudentCredentialDistribution> credentialsList = list.get(0);
            if(credentialsList != null && !credentialsList.isEmpty()) {
                String paperType = credentialsList.get(0).getPaperType();
                summaryDTO.increment(paperType);
                LOGGER.debug("Left:{}\n", summaryDTO.getReadCount() - summaryDTO.getProcessedCount());
                ResponseObj tokenResponse = restUtils.getTokenResponseObject();
                LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
                processGlobalList(summaryDTO.getGlobalList(),jobExecutionId,summaryDTO.getMapDist(),"NONGRADDIST",tokenResponse.getAccess_token());
                LOGGER.info("=======================================================================================");
            }
        }
    }

    @SneakyThrows
    private void processGlobalList(List<StudentCredentialDistribution> cList, Long batchId, Map<String,DistributionPrintRequest> mapDist, String activityCode, String accessToken) {
        List<String> uniqueSchoolList = cList.stream().map(StudentCredentialDistribution::getSchoolOfRecord).distinct().collect(Collectors.toList());
        uniqueSchoolList.forEach(usl->{
            List<StudentCredentialDistribution> yed4List = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen()) && scd.getPaperType().compareTo("YED4") == 0).collect(Collectors.toList());
            List<StudentCredentialDistribution> studentList = cList.stream().filter(scd->scd.getSchoolOfRecord().compareTo(usl)==0 && StringUtils.isNotBlank(scd.getPen())).collect(Collectors.toList());
            supportListener.transcriptPrintFile(yed4List,batchId,usl,mapDist,null);
            schoolDistributionPrintFile(studentList,batchId,usl,mapDist);
        });
        System.out.println(jsonTransformer.marshall(mapDist));
        DistributionResponse disres = restUtils.mergeAndUpload(batchId,accessToken,mapDist,activityCode,"N");
        if(disres != null) {
            ResponseObj obj = restUtils.getTokenResponseObject();
            updateBackStudentRecords(cList,batchId,activityCode,obj.getAccess_token());
        }
    }

    private void schoolDistributionPrintFile(List<StudentCredentialDistribution> studentList, Long batchId, String usl, Map<String,DistributionPrintRequest> mapDist) {
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
            }else{
                DistributionPrintRequest dist = new DistributionPrintRequest();
                dist.setSchoolDistributionRequest(tpReq);
                mapDist.put(usl,dist);
            }
        }
    }

    private void updateBackStudentRecords(List<StudentCredentialDistribution> cList,Long batchId,String activityCode,String accessToken) {
        cList.forEach(scd-> {
            restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,accessToken);
            restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,accessToken);
        });
    }
}
