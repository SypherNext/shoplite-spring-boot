package com.shoplite.legacy;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Prices as plain numbers (12.50), never in scientific notation. */
@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer plainBigDecimals() {
        return builder -> builder.featuresToEnable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

}
