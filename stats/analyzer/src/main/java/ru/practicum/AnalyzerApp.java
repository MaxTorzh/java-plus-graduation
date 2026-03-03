package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class AnalyzerApp {
    public static void main(String[] args) {
        try {
            log.info("Starting Analyzer application with diagnostic mode...");
            log.info("Java version: {}", System.getProperty("java.version"));

            checkClass("ru.practicum.controller.AnalyzerController");
            checkClass("ru.practicum.kafka.ConsumerActionService");
            checkClass("ru.practicum.service.UserActionProcessor");

            ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApp.class, args);

            log.info("==================================================");
            log.info("Analyzer application started successfully!");
            log.info("Active profiles: {}", (Object) context.getEnvironment().getActiveProfiles());
            log.info("==================================================");

        } catch (Exception e) {
            log.error("Failed to start Analyzer application", e);
            e.printStackTrace();
            throw e;
        }
    }

    private static void checkClass(String className) {
        try {
            Class.forName(className);
            log.info("Class found: {}", className);
        } catch (ClassNotFoundException e) {
            log.error("Class NOT found: {}", className);
        }
    }
}