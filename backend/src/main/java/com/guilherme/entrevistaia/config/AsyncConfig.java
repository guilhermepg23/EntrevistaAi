package com.guilherme.entrevistaia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Liga o @Async (usado no AssessmentReportListener). O executor é o padrão do
// Spring Boot (spring.task.execution.*).
@Configuration
@EnableAsync
public class AsyncConfig {
}
