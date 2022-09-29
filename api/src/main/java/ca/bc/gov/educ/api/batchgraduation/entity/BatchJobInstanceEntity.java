package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "BATCH_JOB_INSTANCE")
public class BatchJobInstanceEntity {
    @Id
    @Column(name = "JOB_INSTANCE_ID", nullable = false)
    private Long id;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "JOB_NAME", nullable = false, length = 100)
    private String jobName;

    @Column(name = "JOB_KEY", nullable = false, length = 32)
    private String jobKey;

}