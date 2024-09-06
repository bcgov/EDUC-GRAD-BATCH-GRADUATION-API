package ca.bc.gov.educ.api.batchgraduation.writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

@Slf4j
public class RegenerateSchoolReportsWriter implements ItemWriter<List<String>> {

    @Override
    public void write(Chunk<? extends List<String>> chunk) throws Exception {
        log.info("Regenerate School Reports Writer");
    }

}
