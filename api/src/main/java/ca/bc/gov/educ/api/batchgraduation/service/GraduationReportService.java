package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.ReportGradStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import ca.bc.gov.educ.api.batchgraduation.util.TokenUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	@Qualifier("batchClient")
	WebClient batchWebClient;

	@Autowired
	RESTService restService;
	
	@Autowired
	EducGradBatchGraduationApiConstants constants;

	@Autowired
	JsonTransformer jsonTransformer;

	@Autowired
	TokenUtils tokenUtils;
	
	public Mono<List<StudentCredentialDistribution>> getTranscriptList() {
		return batchWebClient.get().uri(constants.getTranscriptDistributionList())
				.headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
				.retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){});
	}

	public Mono<List<StudentCredentialDistribution>> getTranscriptListYearly() {
		return batchWebClient.get().uri(constants.getTranscriptYearlyDistributionList())
				.headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
				.retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){});
	}

	public Mono<List<StudentCredentialDistribution>> getCertificateList() {
		return batchWebClient.get().uri(constants.getCertificateDistributionList())
				.headers(h -> h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()))
				.retrieve().bodyToMono(new ParameterizedTypeReference<List<StudentCredentialDistribution>>(){});
	}

	public List<PsiCredentialDistribution> getPsiStudentsForRun(String transmissionType,String psiCodes,String psiYear,String accessToken) {
		UUID correlationID = UUID.randomUUID();
		final ParameterizedTypeReference<List<PsiCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
		};
		return batchWebClient.get().uri(String.format(constants.getPsiStudentList(),transmissionType,psiCodes,psiYear))
				.headers(h -> {
					h.setBearerAuth(accessToken);
					h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
				}).retrieve().bodyToMono(responseType).block();
	}

	public List<StudentCredentialDistribution> getStudentsNonGradForYearlyDistribution(String accessToken) {
		var response = restService.get(String.format(constants.getStudentDataNonGradEarly()), List.class, accessToken);
		List<ReportGradStudentData> reportGradStudentDataList = jsonTransformer.convertValue(response, new TypeReference<>(){});
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	// Year-end NonGrad distribution
	public List<StudentCredentialDistribution> getStudentsNonGradForYearlyDistribution(UUID schoolId, String accessToken) {
		var response = restService.get(String.format(constants.getStudentDataNonGradEarlyBySchoolId(), schoolId), List.class, accessToken);
		List<ReportGradStudentData> reportGradStudentDataList = jsonTransformer.convertValue(response, new TypeReference<>(){});
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	// Year-end distribution
	public List<StudentCredentialDistribution> getStudentsForYearlyDistribution(String accessToken) {
		var response = restService.get(String.format(constants.getStudentReportDataYearly()), List.class, accessToken);
		List<ReportGradStudentData> reportGradStudentDataList = jsonTransformer.convertValue(response, new TypeReference<>(){});
		return populateStudentCredentialDistributions(reportGradStudentDataList);
	}

	public List<UUID> getSchoolsNonGradYearly(String accessToken) {
		var response = restService.get(constants.getSchoolDataNonGradEarly(), List.class, accessToken);
		return jsonTransformer.convertValue(response, new TypeReference<>(){});
	}

	public List<UUID> getDistrictsNonGradYearly(String accessToken) {
		var response = restService.get(constants.getDistrictDataNonGradEarly(), List.class, accessToken);
		return jsonTransformer.convertValue(response, new TypeReference<>(){});
	}

	public List<UUID> getDistrictsYearly(String accessToken) {
		var response = restService.get(constants.getDistrictDataYearly(), List.class, accessToken);
		return jsonTransformer.convertValue(response, new TypeReference<>(){});
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
		/** dist.setSchoolId(StringUtils.isBlank(data.getMincodeAtGrad()) ? data.getMincode() : data.getMincodeAtGrad()); **/
		dist.setSchoolId(data.getSchoolOfRecordId());
		//<--
		dist.setSchoolAtGradId(data.getSchoolAtGradId());
		dist.setSchoolOfRecordOriginId(data.getSchoolOfRecordId());
		dist.setDistrictId(data.getDistrictId());
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
		LOGGER.info("Populate Student Credential Distribution for pen {}: SchoolOfRecordOrigin->{}, SchoolAtGrad->{}, SchoolOfRecord->{}", dist.getPen(), dist.getSchoolOfRecordOriginId(), dist.getSchoolAtGradId(), dist.getSchoolId());
		return dist;
	}
}
