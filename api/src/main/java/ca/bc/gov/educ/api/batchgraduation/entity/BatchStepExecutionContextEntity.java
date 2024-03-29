package ca.bc.gov.educ.api.batchgraduation.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "BATCH_STEP_EXECUTION_CONTEXT")
public class BatchStepExecutionContextEntity {

    @Id
    @Column(name = "STEP_EXECUTION_ID", nullable = false)
    private Long stepExecutionId;

    @Column(name = "SHORT_CONTEXT", nullable = false, length = 2500)
    private String shortContext;

    @Lob
    @Column(name = "SERIALIZED_CONTEXT")
    private String serializedContext;

}