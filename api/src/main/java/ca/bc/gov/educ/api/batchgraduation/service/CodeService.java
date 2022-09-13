package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobTypeEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import ca.bc.gov.educ.api.batchgraduation.model.transformer.BatchJobTypeTransformer;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchJobTypeRepository;
import ca.bc.gov.educ.api.batchgraduation.util.GradValidation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Service
public class CodeService {

	@Autowired
	private BatchJobTypeRepository batchJobTypeRepository;

	@Autowired
	private BatchJobTypeTransformer batchJobTypeTransformer;
	
	@Autowired
	GradValidation validation;

	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger(CodeService.class);
	private static final String CREATED_BY="createdBy";
	private static final String CREATED_TIMESTAMP="createdTimestamp";

	@Transactional
	public List<BatchJobType> getAllBatchJobTypeCodeList() {
		return batchJobTypeTransformer.transformToDTO(batchJobTypeRepository.findAll());
	}

	@Transactional
	public BatchJobType getSpecificBatchJobTypeCodeTypeCode(String tranTypeCode) {
		Optional<BatchJobTypeEntity> entity = batchJobTypeRepository.findById(StringUtils.toRootUpperCase(tranTypeCode));
		if (entity.isPresent()) {
			return batchJobTypeTransformer.transformToDTO(entity);
		} else {
			return null;
		}
	}

	public BatchJobType createBatchJobType(@Valid BatchJobType batchJobType) {
		BatchJobTypeEntity toBeSavedObject = batchJobTypeTransformer.transformToEntity(batchJobType);
		Optional<BatchJobTypeEntity> existingObjectCheck = batchJobTypeRepository.findById(batchJobType.getCode());
		if(existingObjectCheck.isPresent()) {
			validation.addErrorAndStop(String.format("Batch Job Type [%s] already exists",batchJobType.getCode()));
			return batchJobType;
		} else {
			return batchJobTypeTransformer.transformToDTO(batchJobTypeRepository.save(toBeSavedObject));
		}
	}

	public BatchJobType updateBatchJobType(@Valid BatchJobType batchJobType) {
		Optional<BatchJobTypeEntity> BatchJobTypeOptional = batchJobTypeRepository.findById(batchJobType.getCode());
		BatchJobTypeEntity sourceObject = batchJobTypeTransformer.transformToEntity(batchJobType);
		if(BatchJobTypeOptional.isPresent()) {
			BatchJobTypeEntity gradEnity = BatchJobTypeOptional.get();
			BeanUtils.copyProperties(sourceObject,gradEnity,CREATED_BY,CREATED_TIMESTAMP);
			return batchJobTypeTransformer.transformToDTO(batchJobTypeRepository.save(gradEnity));
		}else {
			validation.addErrorAndStop(String.format("Certificate Type [%s] does not exists",batchJobType.getCode()));
			return batchJobType;
		}
	}

	public int deleteBatchJobType(@Valid String batchJobType) {
		Optional<BatchJobTypeEntity> entity = batchJobTypeRepository.findById(batchJobType);
		if(entity.isPresent()) {
			batchJobTypeRepository.deleteById(batchJobType);
			return 1;
		}
		return 0;
	}
}
