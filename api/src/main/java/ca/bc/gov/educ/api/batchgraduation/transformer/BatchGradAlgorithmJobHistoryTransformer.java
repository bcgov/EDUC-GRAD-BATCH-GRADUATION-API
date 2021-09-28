package ca.bc.gov.educ.api.batchgraduation.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchGradAlgorithmJobHistory;


@Component
public class BatchGradAlgorithmJobHistoryTransformer {

    @Autowired
    ModelMapper modelMapper;

    public BatchGradAlgorithmJobHistory transformToDTO (BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity) {
    	return modelMapper.map(batchGradAlgorithmJobHistoryEntity, BatchGradAlgorithmJobHistory.class);
    }

    public BatchGradAlgorithmJobHistory transformToDTO ( Optional<BatchGradAlgorithmJobHistoryEntity> batchGradAlgorithmJobHistoryEntity ) {
    	BatchGradAlgorithmJobHistoryEntity cae = new BatchGradAlgorithmJobHistoryEntity();
        if (batchGradAlgorithmJobHistoryEntity.isPresent())
            cae = batchGradAlgorithmJobHistoryEntity.get();

        return modelMapper.map(cae, BatchGradAlgorithmJobHistory.class);
    }

	public List<BatchGradAlgorithmJobHistory> transformToDTO (Iterable<BatchGradAlgorithmJobHistoryEntity> courseEntities ) {
		List<BatchGradAlgorithmJobHistory> programList = new ArrayList<>();
        for (BatchGradAlgorithmJobHistoryEntity courseEntity : courseEntities) {
        	BatchGradAlgorithmJobHistory program = modelMapper.map(courseEntity, BatchGradAlgorithmJobHistory.class);
            programList.add(program);
        }
        return programList;
    }

    public BatchGradAlgorithmJobHistoryEntity transformToEntity(BatchGradAlgorithmJobHistory gradBatchInfoDetails) {
        return modelMapper.map(gradBatchInfoDetails, BatchGradAlgorithmJobHistoryEntity.class);
    }
}
