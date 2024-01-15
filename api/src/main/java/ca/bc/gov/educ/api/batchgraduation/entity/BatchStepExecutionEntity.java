package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "BATCH_STEP_EXECUTION")
public class BatchStepExecutionEntity {

    @Id
    @Column(name = "STEP_EXECUTION_ID", nullable = false)
    private Long stepExecutionId;

    @Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Column(name = "STEP_NAME", nullable = false, length = 100)
    private String stepName;

    @Column(name = "START_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime startTime = LocalDateTime.now();

    @Column(name = "END_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime endTime;

    @Column(name = "STATUS", length = 10)
    private String status;

    @Column(name = "COMMIT_COUNT")
    private Long commitCount;

    @Column(name = "READ_COUNT")
    private Long readCount;

    @Column(name = "FILTER_COUNT")
    private Long filterCount;

    @Column(name = "WRITE_COUNT")
    private Long writeCount;

    @Column(name = "READ_SKIP_COUNT")
    private Long readSkipCount;

    @Column(name = "WRITE_SKIP_COUNT")
    private Long writeSkipCount;

    @Column(name = "PROCESS_SKIP_COUNT")
    private Long processSkipCount;

    @Column(name = "ROLLBACK_COUNT")
    private Long rollbackCount;

    @Column(name = "EXIT_CODE", length = 2500)
    private String exitCode;

    @Column(name = "EXIT_MESSAGE", length = 2500)
    private String exitMessage;

    @Column(name = "LAST_UPDATED")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(name = "CREATE_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createTime = LocalDateTime.now();

}