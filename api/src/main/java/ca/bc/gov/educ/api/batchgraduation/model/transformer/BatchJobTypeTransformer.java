package ca.bc.gov.educ.api.batchgraduation.model.transformer;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobTypeEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Component
public class BatchJobTypeTransformer {

    @Autowired
    ModelMapper modelMapper;

    public BatchJobType transformToDTO (BatchJobTypeEntity entity) {
        return modelMapper.map(entity, BatchJobType.class);
    }

    public BatchJobType transformToDTO ( Optional<BatchJobTypeEntity> entity ) {
        BatchJobTypeEntity cae = new BatchJobTypeEntity();
        if (entity.isPresent())
            cae = entity.get();
        return modelMapper.map(cae, BatchJobType.class);
    }

	public List<BatchJobType> transformToDTO (Iterable<BatchJobTypeEntity> entities ) {
		List<BatchJobType> batchJobTypeList = new ArrayList<>();
        for (BatchJobTypeEntity entity : entities) {
            BatchJobType tTypes = transformToDTO(entity);
            batchJobTypeList.add(tTypes);
        }
        return batchJobTypeList;
    }

    public BatchJobTypeEntity transformToEntity(BatchJobType batchJobType) {
        return modelMapper.map(batchJobType, BatchJobTypeEntity.class);
    }
}
