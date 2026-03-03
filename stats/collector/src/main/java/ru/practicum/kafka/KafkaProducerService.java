package ru.practicum.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Slf4j
@Service
public class KafkaProducerService {

    private final boolean enabled;
    private final String bootstrapServers;
    private final String topic;

    public KafkaProducerService(
            @Value("${kafka.bootstrap-servers:}") String bootstrapServers,
            @Value("${kafka.enabled:false}") boolean enabled,
            @Value("${kafka.topic:user-actions}") String topic) {

        this.bootstrapServers = bootstrapServers;
        this.enabled = enabled && bootstrapServers != null && !bootstrapServers.isEmpty();
        this.topic = topic;

        log.info("=".repeat(50));
        log.info("KafkaProducerService constructor called");
        log.info("bootstrapServers: '{}'", bootstrapServers);
        log.info("enabled: {}", this.enabled);
        log.info("topic: {}", topic);
        log.info("=".repeat(50));
    }

    public void send(SpecificRecordBase action, Long eventId, Instant timestamp, String topic) {
        log.info("SEND METHOD CALLED - Kafka is {}", enabled ? "ENABLED" : "DISABLED");
        log.info("Would send to topic: {}, eventId: {}, action: {}", topic, eventId, action);

        if (enabled) {
            log.info("Kafka is enabled but actual sending is disabled for testing");
        }
    }
}