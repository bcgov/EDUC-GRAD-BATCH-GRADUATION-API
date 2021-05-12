package ca.bc.gov.educ.api.batchgraduation.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
@Entity
@Table(name = "GRAD_SPECIAL_PROGRAM")
public class GradSpecialProgramEntity extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Column(name = "SPECIAL_PROGRAM_CODE", nullable = true)
    private String specialProgramCode;

    @Column(name = "SPECIAL_PROGRAM_NAME", nullable = true)
    private String specialProgramName;

    @Column(name = "FK_GRAD_PROGRAM_CODE", nullable = true)
    private String programCode;

}
