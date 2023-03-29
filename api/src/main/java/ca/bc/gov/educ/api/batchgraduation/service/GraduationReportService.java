package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.ReportGradStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GraduationReportService {

	@Autowired
    WebClient webClient;
	
	@Autowired
	EducGradBatchGraduationApiConstants constants;
	
	public Mono<List<StudentCredentialDistribution>> getTranscriptList(String accessToken) {
		return webClient.get().uri(constants.getTranscriptDistributionList()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>() {
		});
	}

	public Mono<List<StudentCredentialDistribution>> getTranscriptListYearly(String accessToken) {
		return webClient.get().uri(constants.getTranscriptYearlyDistributionList()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){});
	}

	public Mono<List<StudentCredentialDistribution>> getCertificateList(String accessToken) {
		return webClient.get().uri(constants.getCertificateDistributionList()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){});
	}

	public List<PsiCredentialDistribution> getPsiStudentsForRun(String transmissionType,String psiCode,String psiYear,String accessToken) {
		UUID correlationID = UUID.randomUUID();
		final ParameterizedTypeReference<List<PsiCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
		};
		return webClient.get().uri(String.format(constants.getPsiStudentList(),transmissionType,psiCode,psiYear))
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
				}).retrieve().bodyToMono(responseType).block();
	}

	public Mono<List<StudentCredentialDistribution>> getStudentsNonGradYearly(String accessToken) {
		List<ReportGradStudentData> reportGradStudentDataList = webClient.get().uri(constants.getStudentDataNonGradEarly()).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){}).block();
		List<StudentCredentialDistribution> result = new ArrayList<>();
		for(ReportGradStudentData data: reportGradStudentDataList) {
			StudentCredentialDistribution dist = new StudentCredentialDistribution();
			dist.setId(data.getGraduationStudentRecordId());
			dist.setCredentialTypeCode(data.getTranscriptTypeCode());
			dist.setStudentID(data.getGraduationStudentRecordId());
			dist.setPaperType("YED4");
			dist.setSchoolOfRecord(data.getMincode());
			dist.setDocumentStatusCode("CUR");
			dist.setPen(data.getPen());
			dist.setLegalFirstName(data.getFirstName());
			dist.setLegalMiddleNames(data.getMiddleName());
			dist.setLegalLastName(data.getLastName());
			dist.setProgramCompletionDate(data.getProgramCompletionDate());
			dist.setHonoursStanding(data.getHonorsStanding());
			dist.setProgram(data.getProgramCode());
			dist.setStudentGrade(data.getStudentGrade());
			dist.setNonGradReasons(data.getNonGradReasons());
		}
		return Flux.fromIterable(result).collectList();
	}
}
