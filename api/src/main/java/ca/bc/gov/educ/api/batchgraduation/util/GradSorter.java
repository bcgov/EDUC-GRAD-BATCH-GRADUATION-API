package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;

import java.util.Comparator;
import java.util.List;

public class GradSorter {

    public static synchronized void sortStudentCredentialDistributionByNames(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalMiddleNames, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortStudentCredentialDistributionBySchool(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getSchoolOfRecord, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortStudentCredentialDistributionBySchoolAndNames(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getSchoolOfRecord, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalMiddleNames, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortBlankCredentialDistributionBySchoolAndNames(List<BlankCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(BlankCredentialDistribution::getSchoolOfRecord, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortSchoolBySchoolOfRecord(List<String> schools) {
        schools.sort(Comparator.nullsLast(Comparator.naturalOrder()));

    }

}
