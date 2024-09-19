package ca.bc.gov.educ.api.batchgraduation.writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;

@Slf4j
public class RegenerateSchoolReportsWriter implements ItemWriter<String> {

    @Override
    public void write(@NonNull Chunk<? extends String> list) throws Exception {
        if(log.isDebugEnabled()) {
            log.info("Regenerate School Reports Writer: chunk size = {}", list.size());
        }
    }

}
