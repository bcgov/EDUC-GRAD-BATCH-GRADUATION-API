package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateDeserializer;
import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateSerializer;
import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateTimeDeserializer;
import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateTimeSerializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.modelmapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Configuration
public class EducGradBatchApiConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Provider<LocalDateTime> localDateTimeProvider = new AbstractProvider<>() {
            @Override
            protected LocalDateTime get() {
                return LocalDateTime.now();
            }
        };

        Converter<Date, LocalDateTime> toLocalDateTime = new AbstractConverter<>() {
            @Override
            protected LocalDateTime convert(Date date) {
                return (date == null) ? null : date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        };
        modelMapper.createTypeMap(Date.class, LocalDateTime.class);
        modelMapper.addConverter(toLocalDateTime);
        modelMapper.getTypeMap(Date.class, LocalDateTime.class).setProvider(localDateTimeProvider);
        return modelMapper;
    }

    /**
     * Lock provider lock provider.
     *
     * @param jdbcTemplate       the jdbc template
     * @param transactionManager the transaction manager
     * @return the lock provider
     */
    @Bean
    public LockProvider lockProvider(@Autowired JdbcTemplate jdbcTemplate, @Autowired PlatformTransactionManager transactionManager) {
        return new JdbcTemplateLockProvider(jdbcTemplate, transactionManager, "BATCH_SHEDLOCK");
    }

    @Bean
    @Primary
    ObjectMapper jacksonObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalDate.class, new GradLocalDateSerializer());
        simpleModule.addSerializer(LocalDateTime.class, new GradLocalDateTimeSerializer());
        simpleModule.addDeserializer(LocalDate.class, new GradLocalDateDeserializer());
        simpleModule.addDeserializer(LocalDateTime.class, new GradLocalDateTimeDeserializer());
        mapper.findAndRegisterModules();
        mapper.registerModule(simpleModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
    
}
