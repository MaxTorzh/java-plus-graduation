package ru.practicum.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.practicum.kafka.KafkaProducerService;
import ru.practicum.handler.UserActionHandler;

@Slf4j
@Configuration
public class AppConfig {

    @Bean
    @Primary
    public KafkaProducerService kafkaProducerService(
            @Value("${kafka.bootstrap-servers:}") String bootstrapServers,
            @Value("${kafka.enabled:false}") boolean enabled,
            @Value("${kafka.topic:user-actions}") String topic) {

        log.info("Creating KafkaProducerService bean in AppConfig");
        return new KafkaProducerService(bootstrapServers, enabled, topic);
    }

    @Bean
    public UserActionHandler userActionHandler(
            KafkaProducerService kafkaProducerService,
            @Value("${kafka.topic:user-actions}") String topic) {

        log.info("Creating UserActionHandler bean in AppConfig");
        return new UserActionHandler(kafkaProducerService, topic);
    }
}
