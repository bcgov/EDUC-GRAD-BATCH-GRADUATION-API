package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * This table is used to achieve distributed lock between pods, for schedulers.
 */
@Data
@Entity
@Table(name = "BATCH_SHEDLOCK")
public class BatchShedlockEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "LOCK_UNTIL")
    private Date lockUntil;

    @Column(name = "LOCKED_AT")
    private Date lockedAt;

    @Column(name = "LOCKED_BY")
    private String lockedBy;

}
