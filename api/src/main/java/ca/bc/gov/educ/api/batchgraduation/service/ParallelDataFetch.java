package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ParallelDataFetch {
    private static final Logger logger = LoggerFactory.getLogger(ParallelDataFetch.class);

    @Autowired
    GraduationReportService graduationReportService;

    public Mono<DistributionDataParallelDTO> fetchDistributionRequiredData(String accessToken) {
        Mono<List<StudentCredentialDistribution>> transcriptList = graduationReportService.getTranscriptList(accessToken);
        Mono<List<StudentCredentialDistribution>> certificateList = graduationReportService.getCertificateList(accessToken);
        return Mono.zip(transcriptList,certificateList).map(tuple -> new DistributionDataParallelDTO(tuple.getT1(),tuple.getT2()));
    }
}