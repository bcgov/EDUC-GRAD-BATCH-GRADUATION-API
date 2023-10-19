package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class EDWSnapshotPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(EDWSnapshotPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        EdwSnapshotSchoolSummaryDTO totalSummaryDTO = (EdwSnapshotSchoolSummaryDTO)getJobExecution().getExecutionContext().get("edwSnapshotSchoolSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new EdwSnapshotSchoolSummaryDTO();
            getJobExecution().getExecutionContext().put("edwSnapshotSchoolSummaryDTO", totalSummaryDTO);
        }
        List<SnapshotResponse> snapshots = totalSummaryDTO.getGlobalList();
        logger.debug("Total Students: {}", snapshots.size());
        if(!snapshots.isEmpty()) {
            int partitionSize = snapshots.size()/gridSize + 1;
            List<List<SnapshotResponse>> partitions = new LinkedList<>();
            for (int i = 0; i < snapshots.size(); i += partitionSize) {
                partitions.add(snapshots.subList(i, Math.min(i + partitionSize, snapshots.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                EdwSnapshotSummaryDTO summaryDTO = new EdwSnapshotSummaryDTO();
                summaryDTO.setGradYear(totalSummaryDTO.getGradYear());
                List<SnapshotResponse> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            logger.info("Found {} students in total running on {} partitions",snapshots.size(),map.size());
            return map;
        }
        logger.info("No Students Found from Snapshot to process.");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
