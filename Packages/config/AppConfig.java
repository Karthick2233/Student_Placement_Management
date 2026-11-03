package com.college.placement.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig {

    public static final String COMPANY_CACHE = "company";
    public static final String DEPARTMENT_STATS_CACHE = "departmentStats";

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(COMPANY_CACHE, DEPARTMENT_STATS_CACHE);
    }

    @Bean
    ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("placement-scheduler-");
        return scheduler;
    }

    @Bean
    HealthIndicator uploadDirHealthIndicator(@Value("${placement.file.upload-dir:uploads}") String uploadDir) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        return () -> Files.isWritable(path)
                ? Health.up().withDetail("path", path.toString()).build()
                : Health.down().withDetail("path", path.toString()).build();
    }

    @Bean
    InfoContributor appInfoContributor(
            @Value("#{'${spring.application.name}'.toUpperCase()}") String appName,
            @Value("#{T(java.time.LocalDate).now().toString()}") String startedOn) {
        return builder -> builder.withDetail("app", Map.of("name", appName, "startedOn", startedOn));
    }
}
