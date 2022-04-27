package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class ErrorDashBoard {

	private List<ErrorBoard> errorList;
	private Pageable pageable;
	private Integer totalPages;
	private Long totalElements;
	private Integer size;
	private Integer number;
	private Sort sort;
	private Integer numberOfElements;
	
}
