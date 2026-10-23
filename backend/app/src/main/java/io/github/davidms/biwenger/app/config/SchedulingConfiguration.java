package io.github.davidms.biwenger.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on {@code @Scheduled} for the whole app. Unconditional on purpose: switching one job off
 * ({@code biwenger.sync.enabled=false}) must not silently stop every other job.
 */
@Configuration
@EnableScheduling
class SchedulingConfiguration {}
