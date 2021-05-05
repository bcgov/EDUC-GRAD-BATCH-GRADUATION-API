package ca.bc.gov.educ.api.batchgraduation.repository;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvCourseRestrictionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConvCourseRestrictionRepository extends JpaRepository<ConvCourseRestrictionsEntity, UUID> {

    List<ConvCourseRestrictionsEntity> findAll();

    @Query(value="select cr from ConvCourseRestrictionsEntity cr\n" +
						"where cr.restrictionStartDateStr is not null or cr.restrictionEndDateStr is not null")
    List<ConvCourseRestrictionsEntity> findAllWithStartDateOrEndDate();

	List<ConvCourseRestrictionsEntity> findByMainCourseAndMainCourseLevel(String courseCode, String courseLevel);

	@Modifying
	@Query(value="insert into CONV_GRAD_COURSE_RESTRICTIONS(ID, CRSE_MAIN, CRSE_MAIN_LVL, CRSE_RESTRICTED, CRSE_RESTRICTED_LVL, RESTRICTION_START_DT_STR, RESTRICTION_END_DT_STR)\n" +
					"select sys_guid() as ID, c1.crse_code as CRSE_MAIN, c1.crse_level as CRSE_MAIN_LVL,\n" +
					" c2.crse_code as CRSE_RESTRICTED, c2.crse_level as CRSE_RESTRICTED_LVL,\n" +
					" trim(c1.start_restrict_session) as RESTRICTION_START_DT, trim(c1.end_restrict_session) as RESTRICTION_END_DT\n" +
					"from tab_crse c1\n" +
					"join tab_crse c2\n" +
					"on c1.restriction_code = c2.restriction_code\n" +
					"and (c1.crse_code  <> c2.crse_code\n" +
					" or  c1.crse_level <> c2.crse_level)", nativeQuery=true)
	@Transactional
	void loadInitialRawData();
}
