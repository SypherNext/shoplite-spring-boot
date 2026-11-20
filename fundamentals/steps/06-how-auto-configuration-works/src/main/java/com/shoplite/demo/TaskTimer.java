package com.shoplite.demo;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Demo for lesson 05: a bean with its own state (its start time), so every user
 * needs a fresh one. Prototype scope gives a new object each time it is asked for.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TaskTimer {

    private final long startedAt = System.nanoTime();

    long elapsedMillis() {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

}
