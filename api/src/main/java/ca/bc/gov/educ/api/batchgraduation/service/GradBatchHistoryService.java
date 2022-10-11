package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GradBatchHistoryService {

    @Autowired
    private BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

    @Autowired
    private BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;

    @Transactional
    public BatchGradAlgorithmJobHistoryEntity saveGradAlgorithmJobHistory(BatchGradAlgorithmJobHistoryEntity ent) {
        return batchGradAlgorithmJobHistoryRepository.save(ent);
    }

    @Transactional
    public void saveGradAlgorithmErrorHistories(List<BatchGradAlgorithmErrorHistoryEntity> entList) {
        batchGradAlgorithmErrorHistoryRepository.saveAll(entList);
    }
}
