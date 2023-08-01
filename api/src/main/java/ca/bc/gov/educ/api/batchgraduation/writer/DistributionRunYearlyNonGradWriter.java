package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DistributionRunYearlyNonGradWriter extends BaseYearEndWriter implements ItemWriter<List<StudentCredentialDistribution>> {

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) {
        if (!list.isEmpty()) {
            summaryDTO.setCredentialCounter("YED4", summaryDTO.getGlobalList().size());

            // save StudentCredentialDistributions
            String jobType = jobParameters.getString("jobType");
            List<StudentCredentialDistribution> scdList = list.get(0);
            if (scdList != null && !scdList.isEmpty()) {
                scdList.forEach(scd -> distributionService.saveStudentCredentialDistribution(summaryDTO.getBatchId(), jobType, scd));
            }
        }
    }
}
