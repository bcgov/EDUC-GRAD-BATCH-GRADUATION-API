package ca.bc.gov.educ.api.batchgraduation.writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;

import java.util.UUID;

@Slf4j
public class RegenerateSchoolReportsWriter implements ItemWriter<UUID> {

    @Override
    public void write(@NonNull Chunk<? extends UUID> list) throws Exception {
        if(log.isDebugEnabled()) {
            log.info("Regenerate School Reports Writer: chunk size = {}", list.size());
        }
    }

}
