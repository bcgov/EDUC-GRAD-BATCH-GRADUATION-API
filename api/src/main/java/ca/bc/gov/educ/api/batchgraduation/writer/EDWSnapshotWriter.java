package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.EdwGraduationSnapshot;
import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EDWSnapshotWriter implements ItemWriter<List<Pair<String, List<EdwGraduationSnapshot>>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotWriter.class);

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['summary']}")
    EdwSnapshotSummaryDTO summaryDTO;

    @Override
    public void write(Chunk<? extends List<Pair<String, List<EdwGraduationSnapshot>>>> list) {
        if (!list.isEmpty()) {
            LOGGER.debug("List parameter size: {}", list.size());
            Pair<String, List<EdwGraduationSnapshot>> snapshot = list.getItems().get(0).get(0);
            String mincode = snapshot.getLeft();
            List<EdwGraduationSnapshot> snapshotList = snapshot.getRight();
            if (snapshotList != null && !snapshotList.isEmpty()) {
                LOGGER.debug("Mincode {}: Snapshots size: {}", mincode, snapshotList.size());
                int totalSize = snapshotList.size();
                AtomicInteger cnt = new AtomicInteger();
                snapshotList.forEach(item -> {
                    refreshToken();
                    if (item.getStudentID() != null) {
                        cnt.getAndIncrement();
                        LOGGER.debug("  Process a student [{} / {}]: pen = {}, studentID = {}", cnt, totalSize, item.getPen(), item.getStudentID());
                        restUtils.processSnapshot(item, summaryDTO);
                    } else {
                        LOGGER.debug("  Skipped a student: pen = {} does not have a studentID", item.getPen());
                    }
                });
                summaryDTO.getCountMap().put(mincode, (long) snapshotList.size());
            }
        }
    }

    private void refreshToken() {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
        }
    }
}
