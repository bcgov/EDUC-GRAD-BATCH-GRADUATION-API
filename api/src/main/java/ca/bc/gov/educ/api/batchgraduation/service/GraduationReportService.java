package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.ReportGradStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GraduationReportService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraduationReportService.class);

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

	public List<PsiCredentialDistribution> getPsiStudentsForRun(String transmissionType,String psiCodes,String psiYear,String accessToken) {
		UUID correlationID = UUID.randomUUID();
		final ParameterizedTypeReference<List<PsiCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
		};
		return webClient.get().uri(String.format(constants.getPsiStudentList(),transmissionType,psiCodes,psiYear))
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
				}).retrieve().bodyToMono(responseType).block();
	}

	public List<StudentCredentialDistribution> getStudentsNonGradForYearlyDistribution(String accessToken) {
		List<ReportGradStudentData> reportGradStudentDataList = webClient.get().uri(String.format(constants.getStudentDataNonGradEarly())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){}).block();
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	// Year-end NonGrad distribution
	public List<StudentCredentialDistribution> getStudentsNonGradForYearlyDistribution(String mincode, String accessToken) {
		List<ReportGradStudentData> reportGradStudentDataList = webClient.get().uri(String.format(constants.getStudentDataNonGradEarlyByMincode(), mincode)).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){}).block();
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	// Year-end distribution
	public List<StudentCredentialDistribution> getStudentsForYearlyDistribution(String accessToken) {
		List<ReportGradStudentData> reportGradStudentDataList = webClient.get().uri(String.format(constants.getStudentReportDataYearly())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<ReportGradStudentData>>(){}).block();
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	public List<String> getSchoolsNonGradYearly(String accessToken) {
		return webClient.get().uri(String.format(constants.getSchoolDataNonGradEarly())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<String>>(){}).block();
	}

	public List<String> getDistrictsNonGradYearly(String accessToken) {
		return webClient.get().uri(String.format(constants.getDistrictDataNonGradEarly())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<String>>(){}).block();
	}

	public List<String> getDistrictsYearly(String accessToken) {
		return webClient.get().uri(String.format(constants.getDistrictDataYearly())).headers(h -> h.setBearerAuth(accessToken)).retrieve().bodyToMono(new ParameterizedTypeReference<List<String>>(){}).block();
	}

	private List<StudentCredentialDistribution> populateStudentCredentialDistributions(List<ReportGradStudentData> reportGradStudentDataList) {
		List<StudentCredentialDistribution> result = new ArrayList<>();
		for(ReportGradStudentData data: reportGradStudentDataList) {
			if (!"DEC".equalsIgnoreCase(data.getStudentStatus())) {
				result.add(populateStudentCredentialDistribution(data));
			}
		}
		return result;
	}

	private StudentCredentialDistribution populateStudentCredentialDistribution(ReportGradStudentData data) {
		StudentCredentialDistribution dist = new StudentCredentialDistribution();
		dist.setId(data.getGraduationStudentRecordId());
		String paperType = StringUtils.trimToNull(data.getPaperType()) == null ? "YED4" : data.getPaperType();
		if("YED4".equalsIgnoreCase(paperType)) {
			dist.setCredentialTypeCode(data.getTranscriptTypeCode());
		} else {
			dist.setCredentialTypeCode(data.getCertificateTypeCode());
		}
		dist.setStudentID(data.getGraduationStudentRecordId());
		dist.setPaperType(paperType);
		//--> Revert code back to school of record GRAD2-2758
		/** dist.setSchoolOfRecord(StringUtils.isBlank(data.getMincodeAtGrad()) ? data.getMincode() : data.getMincodeAtGrad()); **/
		dist.setSchoolOfRecord(data.getMincode());
		//<--
		dist.setSchoolAtGrad(data.getMincodeAtGrad());
		dist.setSchoolOfRecordOrigin(data.getMincode());
		dist.setDocumentStatusCode("COMPL");
		dist.setPen(data.getPen());
		dist.setLegalFirstName(data.getFirstName());
		dist.setLegalMiddleNames(data.getMiddleName());
		dist.setLegalLastName(data.getLastName());
		dist.setProgramCompletionDate(data.getProgramCompletionDate());
		dist.setHonoursStanding(data.getHonorsStanding());
		dist.setProgram(data.getProgramCode());
		dist.setStudentGrade(data.getStudentGrade());
		dist.setNonGradReasons(data.getNonGradReasons());
		dist.setLastUpdateDate(data.lastUpdateDateAsString());
		LOGGER.info("Populate Student Credential Distribution for pen {}: SchoolOfRecordOrigin->{}, SchoolAtGrad->{}, SchoolOfRecord->{}", dist.getPen(), dist.getSchoolOfRecordOrigin(), dist.getSchoolAtGrad(), dist.getSchoolOfRecord());
		return dist;
	}
}
