package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.EdwGraduationSnapshot;
import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSchoolSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.SnapshotResponse;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EDWSnapshotProcessorTest {

    @Mock
    private RestUtils restUtils;

    @InjectMocks
    private EDWSnapshotProcessor processor;

    @InjectMocks
    private EDWSnapshotSchoolProcessor schoolProcessor;

    @Test
    void testProcessorPassesStudentIdToGraduationApi() throws Exception {
        UUID studentID = UUID.randomUUID();
        UUID schoolOfRecordId = UUID.randomUUID();

        SnapshotResponse snapshot = new SnapshotResponse();
        snapshot.setStudentID(studentID);
        snapshot.setPen("123456789");
        snapshot.setSchoolOfRecord("12345678");
        snapshot.setSchoolOfRecordId(schoolOfRecordId.toString());
        snapshot.setGraduatedDate(null);
        snapshot.setGpa(new BigDecimal("3.75"));
        snapshot.setHonourFlag("Y");

        EdwSnapshotSummaryDTO summaryDTO = new EdwSnapshotSummaryDTO();
        summaryDTO.setGradYear(2026);
        ReflectionTestUtils.setField(processor, "summaryDTO", summaryDTO);
        ReflectionTestUtils.setField(processor, "batchId", 99L);

        EdwGraduationSnapshot returned = new EdwGraduationSnapshot();
        returned.setStudentID(studentID);
        returned.setEligible("Y");

        when(restUtils.processSnapshot(any(EdwGraduationSnapshot.class), same(summaryDTO))).thenReturn(returned);

        EdwGraduationSnapshot result = processor.process(snapshot);

        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
        assertThat(result.getEligible()).isEqualTo("Y");
    }

    @Test
    void testSchoolProcessorReturnsOnlyNonGraduatedStudents() throws Exception {
        SnapshotResponse graduated = new SnapshotResponse();
        graduated.setPen("111111111");
        graduated.setGraduatedDate("202606");

        SnapshotResponse nonGrad = new SnapshotResponse();
        nonGrad.setPen("222222222");
        nonGrad.setGraduatedDate(null);

        SnapshotResponse blankDateNonGrad = new SnapshotResponse();
        blankDateNonGrad.setPen("333333333");
        blankDateNonGrad.setGraduatedDate("");

        EdwSnapshotSchoolSummaryDTO summaryDTO = new EdwSnapshotSchoolSummaryDTO();
        summaryDTO.setGradYear(2026);
        ReflectionTestUtils.setField(schoolProcessor, "summaryDTO", summaryDTO);
        ReflectionTestUtils.setField(schoolProcessor, "batchId", 100L);

        when(restUtils.getEDWSnapshotStudents(2026, "12345678")).thenReturn(List.of(graduated, nonGrad, blankDateNonGrad));

        List<Pair<String, List<SnapshotResponse>>> result = schoolProcessor.process("12345678");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeft()).isEqualTo("12345678");
        assertThat(result.get(0).getRight()).containsExactly(nonGrad, blankDateNonGrad);
    }
}
