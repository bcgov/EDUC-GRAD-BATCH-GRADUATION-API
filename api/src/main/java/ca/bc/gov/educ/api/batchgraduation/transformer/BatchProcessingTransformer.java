package ca.bc.gov.educ.api.batchgraduation.transformer;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessing;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class BatchProcessingTransformer {

    @Autowired
    ModelMapper modelMapper;

    public BatchProcessing transformToDTO (BatchProcessingEntity batchProcessingEntity) {
    	return modelMapper.map(batchProcessingEntity, BatchProcessing.class);
    }

    public BatchProcessing transformToDTO ( Optional<BatchProcessingEntity> batchProcessingEntity ) {
    	BatchProcessingEntity cae = new BatchProcessingEntity();
        if (batchProcessingEntity.isPresent())
            cae = batchProcessingEntity.get();

        return modelMapper.map(cae, BatchProcessing.class);
    }

	public List<BatchProcessing> transformToDTO (Iterable<BatchProcessingEntity> courseEntities ) {
		List<BatchProcessing> programList = new ArrayList<>();
        for (BatchProcessingEntity courseEntity : courseEntities) {
        	BatchProcessing program = modelMapper.map(courseEntity, BatchProcessing.class);
            programList.add(program);
        }
        return programList;
    }

    public BatchProcessingEntity transformToEntity(BatchProcessing gradBatchInfoDetails) {
        return modelMapper.map(gradBatchInfoDetails, BatchProcessingEntity.class);
    }
}
