package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class GradSchoolOfRecordFilter implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(GradSchoolOfRecordFilter.class);

    private final RestUtils restUtils;

    @Autowired
    public GradSchoolOfRecordFilter(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    public List<UUID> filterSchoolsByStudentSearch(StudentSearchRequest searchRequest) {
        List<UUID> eligibleStudentSchoolDistricts = new ArrayList<>();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                logger.debug("Use schoolCategory code {} to find list of schools", schoolCategoryCode);
                List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> schools = restUtils.getSchoolsBySchoolCategoryCode(schoolCategoryCode);
                for(ca.bc.gov.educ.api.batchgraduation.model.institute.School school: schools) {
                    logger.debug("SchoolId {} / Mincode {} found by schoolCategory code {}", school.getSchoolId(), school.getMincode(), schoolCategoryCode);
                    eligibleStudentSchoolDistricts.add(UUID.fromString(school.getSchoolId()));
                }
            }
        }
        if(searchRequest != null && searchRequest.getDistrictIds() != null && !searchRequest.getDistrictIds().isEmpty()) {
            if(!eligibleStudentSchoolDistricts.isEmpty()) {
                eligibleStudentSchoolDistricts.removeIf(scr -> !searchRequest.getDistrictIds().contains(scr));
            } else {
                for(UUID districtId: searchRequest.getDistrictIds()) {
                    logger.debug("Use district id {} to find list of schools", districtId);
                    List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> schools = restUtils.getSchoolsByDistrictId(districtId);
                    for(ca.bc.gov.educ.api.batchgraduation.model.institute.School school: schools) {
                        logger.debug("School {} found by district id {}", school.getMincode(), school.getDistrictId());
                        eligibleStudentSchoolDistricts.add(UUID.fromString(school.getSchoolId()));
                    }
                }
            }
        }
        if(searchRequest != null && searchRequest.getSchoolIds() != null && !searchRequest.getSchoolIds().isEmpty()) {
            if(!eligibleStudentSchoolDistricts.isEmpty()) {
                eligibleStudentSchoolDistricts.removeIf(scr -> !searchRequest.getSchoolIds().contains(scr));
            } else {
                eligibleStudentSchoolDistricts = searchRequest.getSchoolIds();
            }
        }
        return eligibleStudentSchoolDistricts;
    }

    public List<UUID> filterStudents(StudentSearchRequest searchRequest) {
        List<UUID> eligibleStudentGuids = new ArrayList<>(searchRequest.getStudentIDs());
        eligibleStudentGuids.addAll(restUtils.getStudentsForSpecialGradRun(searchRequest));
        return eligibleStudentGuids;
    }

}
