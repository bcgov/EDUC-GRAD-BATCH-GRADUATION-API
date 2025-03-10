package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.model.institute.School;
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
        List<School> eligibleSchools = new ArrayList<>();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                logger.debug("Use schoolCategory code {} to find list of schools", schoolCategoryCode);
                eligibleSchools.addAll(restUtils.getSchoolsBySchoolCategoryCode(schoolCategoryCode));
                logger.debug("Found {} schools by schoolCategory code {}", eligibleSchools.size(), schoolCategoryCode);
            }
        }
        if(searchRequest != null && searchRequest.getDistrictIds() != null && !searchRequest.getDistrictIds().isEmpty()) {
            if(!eligibleSchools.isEmpty()) {
                eligibleSchools.removeIf(scr -> !searchRequest.getDistrictIds().contains(UUID.fromString(scr.getDistrictId())));
            } else {
                for(UUID districtId: searchRequest.getDistrictIds()) {
                    logger.debug("Use district id {} to find list of schools", districtId);
                    eligibleSchools = restUtils.getSchoolsByDistrictId(districtId);
                    logger.debug("Found {} schools by district id {}", eligibleSchools.size(), districtId);
                }
            }
        }
        if(searchRequest != null && searchRequest.getSchoolIds() != null && !searchRequest.getSchoolIds().isEmpty()) {
            if(!eligibleSchools.isEmpty()) {
                eligibleSchools.removeIf(scr -> !searchRequest.getSchoolIds().contains(UUID.fromString(scr.getSchoolId())));
            } else {
                return searchRequest.getSchoolIds();
            }
        }
        return eligibleSchools.stream().map(scr -> UUID.fromString(scr.getSchoolId())).toList();
    }

    public List<UUID> filterStudents(StudentSearchRequest searchRequest) {
        List<UUID> eligibleStudentGuids = new ArrayList<>(searchRequest.getStudentIDs());
        eligibleStudentGuids.addAll(restUtils.getStudentsForSpecialGradRun(searchRequest));
        return eligibleStudentGuids;
    }

}
