package ca.bc.gov.educ.api.batchgraduation.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SkipSQLTransactionExceptionsListener implements org.springframework.batch.core.SkipListener<Object, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkipSQLTransactionExceptionsListener.class);

    @Override
    public void onSkipInProcess(Object arg0, Throwable throwable) {
        LOGGER.info("====> Processor: skip this exception <====  {}", throwable.getLocalizedMessage());
    }

    @Override
    public void onSkipInRead(Throwable throwable) {
        LOGGER.info("====> Reader: skip this exception <====  {}", throwable.getLocalizedMessage());
    }

    @Override
    public void onSkipInWrite(Object arg0, Throwable throwable) {
        LOGGER.info("====> Writer: skip this exception <====  {}", throwable.getLocalizedMessage());
    }
}
