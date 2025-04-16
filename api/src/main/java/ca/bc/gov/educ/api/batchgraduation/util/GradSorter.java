package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class GradSorter {

    public static synchronized void sortStudentCredentialDistributionByNames(List<? extends StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalMiddleNames, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortStudentCredentialDistributionBySchool(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getSchoolId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortStudentCredentialDistributionBySchoolAndNames(List<StudentCredentialDistribution> students) {
        students.sort(Comparator
                .comparing(StudentCredentialDistribution::getSchoolId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalLastName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalFirstName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(StudentCredentialDistribution::getLegalMiddleNames, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    public static synchronized void sortSchoolBySchoolOfRecordId(List<UUID> schools) {
        schools.sort(Comparator.nullsLast(Comparator.naturalOrder()));
    }

}
