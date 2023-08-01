package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class CommonSchool {

    /**
     * The constant serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    private String distNo;
    private String schlNo;
    private String scAddressLine1;
    private String scAddressLine2;
    private String scCity;
    private String scProvinceCode;
    private String scCountryCode;
    private String scPostalCode;
    private String scFaxNumber;
    private String scPhoneNumber;
    private String scEMailId;
    private String facilityTypeCode;
    private String schoolName;
    private String schoolTypeCode;
    private String schoolOrganizationCode;
    private String schoolCategoryCode;
    private String prGivenName;
    private String prSurname;
    private String prMiddleName;
    private String prTitleCode;
    private Long numberOfDivisions;
    private Long numberOfSecFteTeachers;
    private Long numberOfElmFteTeachers;
    private Long ttblElemInstrMinutes;
    private String schoolStatusCode;
    private Long enrolHeadcount1523;
    private Long enrolHeadcount1701;
    private String grade01Ind;
    private String grade29Ind;
    private String grade04Ind;
    private String grade05Ind;
    private String grade06Ind;
    private String grade07Ind;
    private String grade08Ind;
    private String grade09Ind;
    private String grade10Ind;
    private String grade11Ind;
    private String grade12Ind;
    private String grade79Ind;
    private String grade89Ind;
    private String openedDate;
    private String closedDate;
    private String authNumber;
    private Long createDate;
    private Long createTime;
    private String createUsername;
    private Long editDate;
    private Long editTime;
    private String editUsername;
    private Long elemTeachersHc;
    private Long secTeachersHc;
    private String gradeKhInd;
    private String gradeKfInd;
    private String grade02Ind;
    private String grade03Ind;
    private String gradeEuInd;
    private String gradeSuInd;
    private String gradeHsInd;
    private String contedFundFlag;
    private Long elemFteClassroom;
    private Long elemFteSupport;
    private Long elemFteAdmin;
    private Long secFteClassroom;
    private Long secFteSupport;
    private Long secFteAdmin;
    private String physAddressLine1;
    private String physAddressLine2;
    private String physCity;
    private String physProvinceCode;
    private String physCountryCode;
    private String physPostalCode;
    private Long educMethodClassCnt;
    private Long educMethodDelCnt;
    private Long educMethodBothCnt;
    private String newDistno;
    private String newSchlno;
    private String dateOpened;
    private String dateClosed;
    private Long assetNumber;
    private String assetAssignedBy;
    private String assetAssignedDate;
    private String assetChangedBy;
    private String assetChangedDate;
    private String restrictFunding;
    private String gradeGaInd;
    private String nlcEarlyLearningFlag;
    private String nlcAfterSchoolProgramFlag;
    private String nlcContinuingEdFlag;
    private String nlcSeniorsFlag;
    private String nlcSportAndRecFlag;
    private String nlcCommunityUseFlag;
    private String nlcIntegratedServicesFlag;
}