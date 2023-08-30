package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@Entity
@Table(name = "BATCH_JOB_EXECUTION_PARAMS")
@EqualsAndHashCode(callSuper=false)
public class BatchJobExecutionParamEntity implements Serializable {

    @Id
    @Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

    @Column(name = "PARAMETER_TYPE", nullable = false, length = 32)
    private String parameterType;

    @Id
    @Column(name = "PARAMETER_NAME", nullable = false, length = 100)
    private String parameterName;

    @Column(name = "PARAMETER_VALUE", length = 1000)
    private String parameterValue;

    @Column(name = "IDENTIFYING", nullable = false)
    private Boolean identifying = false;

}