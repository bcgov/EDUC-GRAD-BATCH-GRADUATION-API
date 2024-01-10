package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ParallelDataFetch {

    @Autowired
    GraduationReportService graduationReportService;

    @Autowired
    RestUtils restUtils;

    // Monthly distribution
    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredData() {
        String accessToken = restUtils.getAccessToken();
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptList(accessToken);
        accessToken = restUtils.getAccessToken();
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList(accessToken);
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    // Supplemental distribution
    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredDataYearly() {
        String accessToken = restUtils.getAccessToken();
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptListYearly(accessToken);
        accessToken = restUtils.getAccessToken();
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList(accessToken);
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    // Year-end distribution
    public List<StudentCredentialDistribution> fetchStudentCredentialsDistributionDataYearly() {
        return restUtils.fetchDistributionRequiredDataStudentsYearly();
    }

    public List<StudentCredentialDistribution> fetchStudentCredentialsDistributionDataYearlyNonGrad() {
        return restUtils.fetchDistributionRequiredDataStudentsNonGradYearly();
    }

    public List<String> fetchDistributionRequiredDataSchoolsNonGradYearly() {
        String accessToken = restUtils.getAccessToken();
        return graduationReportService.getSchoolsNonGradYearly(accessToken);
    }

    public List<String> fetchDistributionRequiredDataDistrictsNonGradYearly() {
        String accessToken = restUtils.getAccessToken();
        return graduationReportService.getDistrictsNonGradYearly(accessToken);
    }

}
