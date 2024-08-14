package ca.bc.gov.educ.api.batchgraduation.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class ArchiveStudentsWriter implements ItemWriter<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsWriter.class);
    @Override
    public void write(Chunk<? extends List<String>> chunk) throws Exception {
        LOGGER.info("Archive Students Writer");
    }
}