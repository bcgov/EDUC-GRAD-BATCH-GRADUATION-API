package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.PsiDistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public abstract class PsiDistributionRunBaseReader implements ItemReader<PsiCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PsiDistributionRunBaseReader.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['summary']}")
    PsiDistributionSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    protected void aggregate(String summaryContextName) {
        PsiDistributionSummaryDTO totalSummaryDTO = (PsiDistributionSummaryDTO)jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new PsiDistributionSummaryDTO();
            totalSummaryDTO.initializeCredentialCountMap();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().addAll(summaryDTO.getErrors());
        totalSummaryDTO.getGlobalList().addAll(summaryDTO.getGlobalList());
        mergeMapCounts(totalSummaryDTO.getCredentialCountMap(),summaryDTO.getCredentialCountMap());
    }

    protected void fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
        }
    }

    private void mergeMapCounts(Map<String, Long> total, Map<String, Long> current) {
        current.forEach((k,v) -> {
            if (total.containsKey(k)) {
                total.put(k, total.get(k) + v);
            } else {
                total.put(k, v);
            }
        });
    }
}