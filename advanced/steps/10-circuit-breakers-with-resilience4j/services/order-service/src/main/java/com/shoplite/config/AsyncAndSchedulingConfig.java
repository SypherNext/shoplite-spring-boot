package com.shoplite.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Async (run a method on another thread) and @Scheduled (run a method on a timer).
 * With spring.threads.virtual.enabled=true, Spring Boot backs both with virtual threads.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class AsyncAndSchedulingConfig {

    /**
     * Copies the caller's context (the current trace and span, logging MDC) onto the @Async thread.
     * Without it, work done in an @Async method starts a fresh trace with no link to the request.
     * Spring Boot applies a TaskDecorator bean to its auto-configured task executor.
     */
    @Bean
    TaskDecorator contextPropagatingTaskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }

}
