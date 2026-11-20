package com.shoplite.common;

import org.springframework.context.annotation.Configuration;
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
}
