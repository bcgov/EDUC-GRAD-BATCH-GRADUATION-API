package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "BATCH_JOB_EXECUTION_CONTEXT")
@EqualsAndHashCode(callSuper=false)
public class BatchJobExecutionContextEntity {
    @Id
    @Column(name = "JOB_EXECUTION_ID", nullable = false)
    private Long jobExecutionId;

    @Column(name = "SHORT_CONTEXT", nullable = false, length = 2500)
    private String shortContext;

    @Lob
    @Column(name = "SERIALIZED_CONTEXT")
    private String serializedContext;

}