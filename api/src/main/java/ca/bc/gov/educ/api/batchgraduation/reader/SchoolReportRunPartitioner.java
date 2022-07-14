package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SchoolReportRunPartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchoolReportRunPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Autowired
    GraduationReportService graduationReportService;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        List<SchoolReportDistribution> schoolReportList = graduationReportService.getSchoolReportForPosting(accessToken);
        if(!schoolReportList.isEmpty()) {
            int partitionSize = schoolReportList.size()/gridSize + 1;
            List<List<SchoolReportDistribution>> partitions = new LinkedList<>();
            for (int i = 0; i < schoolReportList.size(); i += partitionSize) {
                partitions.add(schoolReportList.subList(i, Math.min(i + partitionSize, schoolReportList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                SchoolReportSummaryDTO summaryDTO = new SchoolReportSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                List<SchoolReportDistribution> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",schoolReportList.size(),map.size());
            return map;
        }
        LOGGER.info("No School Reports Found for Processing");
        return new HashMap<>();
    }
}
