package org.trellis.core;

/**
 * Auto-configuration for the Trellis Core framework. When this library is added to a Spring Boot
 * application's classpath, this class automatically bootstraps all Trellis infrastructure (Aspects,
 * Components) without requiring the consumer to use {@code @ComponentScan}.
 */
@org.springframework.boot.autoconfigure.AutoConfiguration
@org.springframework.context.annotation.ComponentScan("org.trellis.core")
public class TrellisAutoConfiguration {
}
