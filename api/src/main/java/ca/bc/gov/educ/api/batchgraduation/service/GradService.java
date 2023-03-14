package ca.bc.gov.educ.api.batchgraduation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class GradService {

    private static final Logger logger = LoggerFactory.getLogger(GradService.class);

    private Instant start;

    void start() {
        start = Instant.now();
    }

    void end() {
        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        logger.debug("Time taken: {} milliseconds", timeElapsed.toMillis());
    }
}
