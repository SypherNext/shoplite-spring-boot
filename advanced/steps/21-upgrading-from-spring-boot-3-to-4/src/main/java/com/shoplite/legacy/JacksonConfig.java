package com.shoplite.legacy;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.core.StreamWriteFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Prices as plain numbers (12.50), never in scientific notation. */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer plainBigDecimals() {
        return builder -> builder.enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

}
