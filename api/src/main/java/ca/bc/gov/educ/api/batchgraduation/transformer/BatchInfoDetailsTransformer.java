package ca.bc.gov.educ.api.batchgraduation.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchInfoDetailsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchInfoDetails;


@Component
public class BatchInfoDetailsTransformer {

    @Autowired
    ModelMapper modelMapper;

    public BatchInfoDetails transformToDTO (BatchInfoDetailsEntity gradBatchInfoDetailsEntity) {
    	return modelMapper.map(gradBatchInfoDetailsEntity, BatchInfoDetails.class);
    }

    public BatchInfoDetails transformToDTO ( Optional<BatchInfoDetailsEntity> gradBatchInfoDetailsEntity ) {
    	BatchInfoDetailsEntity cae = new BatchInfoDetailsEntity();
        if (gradBatchInfoDetailsEntity.isPresent())
            cae = gradBatchInfoDetailsEntity.get();

        return modelMapper.map(cae, BatchInfoDetails.class);
    }

	public List<BatchInfoDetails> transformToDTO (Iterable<BatchInfoDetailsEntity> courseEntities ) {
		List<BatchInfoDetails> programList = new ArrayList<>();
        for (BatchInfoDetailsEntity courseEntity : courseEntities) {
        	BatchInfoDetails program = modelMapper.map(courseEntity, BatchInfoDetails.class);
            programList.add(program);
        }
        return programList;
    }

    public BatchInfoDetailsEntity transformToEntity(BatchInfoDetails gradBatchInfoDetails) {
        return modelMapper.map(gradBatchInfoDetails, BatchInfoDetailsEntity.class);
    }
}
