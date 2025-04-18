package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class ParallelDataFetch {

    @Autowired
    GraduationReportService graduationReportService;

    @Autowired
    RestUtils restUtils;

    // Monthly distribution
    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredData() {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptList();
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList();
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    // Supplemental distribution
    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredDataYearly() {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptListYearly();
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList();
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    // Year-end distribution
    public List<StudentCredentialDistribution> fetchStudentCredentialsDistributionDataYearly() {
        return restUtils.fetchDistributionRequiredDataStudentsYearly();
    }
    public List<StudentCredentialDistribution> fetchYearEndStudentCredentials(StudentSearchRequest studentSearchRequest) {
        return restUtils.fetchDistributionRequiredDataStudentsYearlyBySearchCriteria(studentSearchRequest);
    }

    public List<StudentCredentialDistribution> fetchStudentCredentialsDistributionDataYearlyNonGrad() {
        return restUtils.fetchDistributionRequiredDataStudentsNonGradYearly();
    }

    public List<UUID> fetchDistributionRequiredDataSchoolsNonGradYearly() {
        String accessToken = restUtils.getAccessToken();
        return graduationReportService.getSchoolsNonGradYearly(accessToken);
    }

    public List<UUID> fetchDistributionRequiredDataDistrictsNonGradYearly() {
        String accessToken = restUtils.getAccessToken();
        return graduationReportService.getDistrictsNonGradYearly(accessToken);
    }

}
