package ru.practicum.kafka;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import serializer.AvroSerializer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaProducerService implements AutoCloseable {
    KafkaProducer<Long, SpecificRecordBase> producer;
    final String bootstrapServers;
    final boolean enabled;

    public KafkaProducerService(
            @Value("${kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${kafka.enabled:true}") boolean enabled) {

        this.bootstrapServers = bootstrapServers;
        this.enabled = enabled && bootstrapServers != null && !bootstrapServers.isEmpty();

        log.info("==================================================");
        log.info("KafkaProducerService created");
        log.info("bootstrapServers: '{}'", bootstrapServers);
        log.info("enabled: {}", this.enabled);
        log.info("==================================================");

        if (this.enabled) {
            createProducer();
        }
    }

    private void createProducer() {
        try {
            Properties config = new Properties();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
            config.put(ProducerConfig.ACKS_CONFIG, "all");
            config.put(ProducerConfig.RETRIES_CONFIG, 3);

            this.producer = new KafkaProducer<>(config);
            log.info("Kafka producer created successfully");
        } catch (Exception e) {
            log.error("Failed to create Kafka producer", e);
            throw new RuntimeException("Failed to create Kafka producer", e);
        }
    }

    @PostConstruct
    void init() {
        log.info("KafkaProducerService initialized");
    }

    public void send(SpecificRecordBase action, Long eventId, Instant timestamp, String topic) {
        if (!enabled || producer == null) {
            log.info("Kafka disabled - would send to topic {}: eventId={}, action={}",
                    topic, eventId, action);
            return;
        }

        try {
            ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                    topic,
                    null,
                    timestamp.toEpochMilli(),
                    eventId,
                    action
            );

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send message to topic {}", topic, exception);
                } else {
                    log.info("Message sent to topic {} partition {} offset {}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            producer.flush();
            log.debug("Message flushed to topic {}", topic);
        } catch (Exception e) {
            log.error("Error sending message to Kafka", e);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        log.info("Closing KafkaProducerService...");
        if (enabled && producer != null) {
            try {
                producer.flush();
                producer.close(Duration.ofSeconds(5));
                log.info("Kafka producer closed successfully");
            } catch (Exception e) {
                log.error("Error closing Kafka producer", e);
            }
        }
    }
}