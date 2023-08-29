package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;

@Data
@Entity
@Table(name = "BATCH_JOB_EXECUTION_PARAMS")
@EqualsAndHashCode(callSuper=false)
public class BatchJobExecutionParamEntity implements Serializable {

    @Id
    @Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

    @Column(name = "TYPE_CD", nullable = false, length = 6)
    private String typeCd;

    @Id
    @Column(name = "KEY_NAME", nullable = false, length = 100)
    private String keyName;

    @Column(name = "STRING_VAL", length = 1000)
    private String stringVal;

    @Column(name = "DATE_VAL")
    private Instant dateVal;

    @Column(name = "LONG_VAL")
    private Long longVal;

    @Column(name = "DOUBLE_VAL")
    private Long doubleVal;

    @Column(name = "IDENTIFYING", nullable = false)
    private Boolean identifying = false;

}