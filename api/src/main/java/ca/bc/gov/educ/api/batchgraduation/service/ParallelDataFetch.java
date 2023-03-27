package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ParallelDataFetch {

    @Autowired
    GraduationReportService graduationReportService;

    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredData(String accessToken) {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptList(accessToken);
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList(accessToken);
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredDataYearly(String accessToken) {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptListYearly(accessToken);
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList(accessToken);
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }

    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredDataNonGradYearly(String accessToken) {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptListYearly(accessToken);
        Mono<List<StudentCredentialDistribution>> certificateList = Mono.empty();
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }
}
