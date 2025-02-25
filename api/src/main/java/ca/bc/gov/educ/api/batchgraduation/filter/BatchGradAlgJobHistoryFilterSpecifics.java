package ca.bc.gov.educ.api.batchgraduation.filter;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class BatchGradAlgJobHistoryFilterSpecifics extends BaseFilterSpecs<BatchGradAlgorithmJobHistoryEntity> {
    /**
     * Instantiates a new Base filter specs.
     *
     * @param chronoLocalDateFilterSpecifications     the date filter specifications
     * @param chronoLocalDateTimeFilterSpecifications the date time filter specifications
     * @param integerFilterSpecifications             the integer filter specifications
     * @param stringFilterSpecifications              the string filter specifications
     * @param longFilterSpecifications                the long filter specifications
     * @param uuidFilterSpecifications                                  the uuid filter specifications
     * @param converters                                                the converters
     */
    public BatchGradAlgJobHistoryFilterSpecifics(FilterSpecifications<BatchGradAlgorithmJobHistoryEntity, ChronoLocalDate> chronoLocalDateFilterSpecifications,
                                                 FilterSpecifications<BatchGradAlgorithmJobHistoryEntity,ChronoLocalDateTime<?>> chronoLocalDateTimeFilterSpecifications,
                                                 FilterSpecifications<BatchGradAlgorithmJobHistoryEntity, Integer> integerFilterSpecifications,
                                                 FilterSpecifications<BatchGradAlgorithmJobHistoryEntity, String> stringFilterSpecifications,
                                                 FilterSpecifications<BatchGradAlgorithmJobHistoryEntity, Long> longFilterSpecifications,
                                                 FilterSpecifications<BatchGradAlgorithmJobHistoryEntity, UUID> uuidFilterSpecifications,
                                                 Converters converters) {
        super(chronoLocalDateFilterSpecifications, chronoLocalDateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
    }
}
