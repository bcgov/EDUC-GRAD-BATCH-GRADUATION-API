package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
public class GradSchoolOfRecordFilter implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GradSchoolOfRecordFilter.class);

    private final RestUtils restUtils;

    @Autowired
    public GradSchoolOfRecordFilter(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public List<String> filterSchoolOfRecords(StudentSearchRequest searchRequest) {
        List<String> eligibleStudentSchoolDistricts = new ArrayList<>();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                logger.debug("Use schoolCategory code {} to find list of schools", schoolCategoryCode);
                List<School> schools = restUtils.getSchoolBySchoolCategoryCode(schoolCategoryCode);
                for(School school: schools) {
                    logger.debug("School {} found by schoolCategory code {}", school.getMincode(), schoolCategoryCode);
                    eligibleStudentSchoolDistricts.add(school.getMincode());
                }
            }
        }
        if(searchRequest != null && searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty()) {
            if(!eligibleStudentSchoolDistricts.isEmpty()) {
                eligibleStudentSchoolDistricts.removeIf(scr -> !searchRequest.getDistricts().contains(StringUtils.substring(scr, 0, 3)));
            } else {
                for(String district: searchRequest.getDistricts()) {
                    logger.debug("Use district code {} to find list of schools", district);
                    List<School> schools = restUtils.getSchoolByDistrictCode(district);
                    for(School school: schools) {
                        logger.debug("School {} found by district code {}", school.getMincode(), district);
                        eligibleStudentSchoolDistricts.add(school.getMincode());
                    }
                }
            }
        }
        if(searchRequest != null && searchRequest.getSchoolOfRecords() != null && !searchRequest.getSchoolOfRecords().isEmpty()) {
            if(!eligibleStudentSchoolDistricts.isEmpty()) {
                eligibleStudentSchoolDistricts.removeIf(scr -> !searchRequest.getSchoolOfRecords().contains(scr));
            } else {
                eligibleStudentSchoolDistricts = searchRequest.getSchoolOfRecords();
            }
        }
        return eligibleStudentSchoolDistricts;
    }

}
