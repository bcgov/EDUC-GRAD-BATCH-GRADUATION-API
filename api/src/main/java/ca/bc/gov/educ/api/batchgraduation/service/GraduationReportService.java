package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.*;
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

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.api.batchgraduation.constants.ReportingSchoolTypesEnum.SCHOOL_AT_GRAD;

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

	public List<YearEndStudentCredentialDistribution> getStudentsForYearlyDistributionBySearchCriteria(String accessToken, StudentSearchRequest searchRequest) {
		var response = restService.post(String.format(constants.getStudentReportDataYearly()), searchRequest, List.class, accessToken);
		List<ReportGradStudentData> reportGradStudentDataList = jsonTransformer.convertValue(response, new TypeReference<>(){});
		return populateYearEndStudentCredentialDistributions(reportGradStudentDataList);
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

	private <T extends StudentCredentialDistribution> List<T> populateStudentCredentialDistributions(
			List<ReportGradStudentData> dataList,
			Function<ReportGradStudentData, T> mapper) {
		return dataList.stream()
				.filter(data -> !"DEC".equalsIgnoreCase(data.getStudentStatus()))
				.map(mapper).collect(Collectors.toList());
	}


	private List<StudentCredentialDistribution> populateStudentCredentialDistributions(List<ReportGradStudentData> reportGradStudentDataList) {
		return populateStudentCredentialDistributions(reportGradStudentDataList,  this::populateStudentCredentialDistribution);
	}

	private List<YearEndStudentCredentialDistribution> populateYearEndStudentCredentialDistributions(List<ReportGradStudentData> reportGradStudentDataList) {
		return populateStudentCredentialDistributions(reportGradStudentDataList,  this::populateYearEndStudentCredentialDistribution);
	}

	private StudentCredentialDistribution populateStudentCredentialDistribution(ReportGradStudentData data) {
		return populateStudentCredentialDistribution(data, new StudentCredentialDistribution());
	}

	private YearEndStudentCredentialDistribution populateYearEndStudentCredentialDistribution(ReportGradStudentData data) {
		YearEndStudentCredentialDistribution dist = populateStudentCredentialDistribution(data, new YearEndStudentCredentialDistribution());
		dist.setSchoolOfRecordId(data.getSchoolOfRecordId());
		dist.setReportingSchoolTypeCode(data.getReportingSchoolTypeCode());
		dist.setDistrictAtGradId(data.getSchoolAtGradId());
		dist.setCertificateTypeCode(data.getCertificateTypeCode());
		dist.setTranscriptTypeCode(data.getTranscriptTypeCode());
		LOGGER.info("Populate Student Credential Distribution for pen {}: SchoolOfRecordOrigin->{}, SchoolAtGrad->{}, SchoolOfRecord->{}", dist.getPen(), dist.getSchoolOfRecordOriginId(), dist.getSchoolAtGradId(), dist.getSchoolId());
		return dist;
	}

	private <T extends StudentCredentialDistribution> T populateStudentCredentialDistribution(ReportGradStudentData data, T dist) {
		dist.setId(data.getGraduationStudentRecordId());
		String paperType = StringUtils.trimToNull(data.getPaperType()) == null ? "YED4" : data.getPaperType();
		if("YED4".equalsIgnoreCase(paperType)) {
			dist.setCredentialTypeCode(data.getTranscriptTypeCode());
		} else {
			dist.setCredentialTypeCode(data.getCertificateTypeCode());
		}
		dist.setStudentID(data.getGraduationStudentRecordId());
		dist.setPaperType(paperType);
		if(data.getReportingSchoolTypeCode() != null && data.getReportingSchoolTypeCode().equalsIgnoreCase(SCHOOL_AT_GRAD.name())) {
			dist.setSchoolId(data.getSchoolAtGradId());
			dist.setDistrictId(data.getDistrictAtGradId());
		} else {
			dist.setSchoolId(data.getSchoolOfRecordId());
			dist.setDistrictId(data.getDistrictId());
		}
		dist.setSchoolAtGradId(data.getSchoolAtGradId());
		dist.setSchoolOfRecordOriginId(data.getSchoolOfRecordId());
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
		dist.setDistrictId(data.getDistrictId());
		LOGGER.info("Populate Student Credential Distribution for pen {}: SchoolOfRecordOrigin->{}, SchoolAtGrad->{}, SchoolOfRecord->{}", dist.getPen(), dist.getSchoolOfRecordOriginId(), dist.getSchoolAtGradId(), dist.getSchoolId());
		return dist;
	}

}
