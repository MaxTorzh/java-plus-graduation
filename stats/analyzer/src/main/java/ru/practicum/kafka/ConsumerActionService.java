package ru.practicum.kafka;

import deserializer.UserActionDeserializer;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConsumerActionService implements AutoCloseable {
    KafkaConsumer<Long, SpecificRecordBase> consumer;
    final String bootstrapServers;
    final String groupId;
    final String autoCommit;
    final boolean enabled;

    public ConsumerActionService(
            @Value("${kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
            @Value("${kafka.group-id.action:analyzer-action-group}") String groupId,
            @Value("${kafka.auto-commit:true}") String autoCommit,
            @Value("${kafka.enabled:true}") boolean enabled) {

        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.autoCommit = autoCommit;
        this.enabled = enabled && bootstrapServers != null && !bootstrapServers.isEmpty();

        log.info("ConsumerActionService created - enabled: {}, bootstrapServers: '{}'",
                this.enabled, bootstrapServers);

        if (this.enabled) {
            createConsumer();
        }
    }

    private void createConsumer() {
        try {
            Properties config = new Properties();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class.getName());

            this.consumer = new KafkaConsumer<>(config);
            log.info("Kafka action consumer created successfully");
        } catch (Exception e) {
            log.error("Failed to create Kafka action consumer", e);
            throw new RuntimeException("Failed to create Kafka action consumer", e);
        }
    }

    @PostConstruct
    void init() {
        log.info("ConsumerActionService initialized");
    }

    public ConsumerRecords<Long, SpecificRecordBase> poll(Duration duration) {
        if (!enabled || consumer == null) {
            return ConsumerRecords.empty();
        }
        return consumer.poll(duration);
    }

    public void subscribe(List<String> topics) {
        if (!enabled || consumer == null) {
            log.debug("Kafka disabled - skipping subscribe to {}", topics);
            return;
        }
        consumer.subscribe(topics);
    }

    public void commitAsync() {
        if (!enabled || consumer == null) {
            return;
        }
        consumer.commitAsync();
    }

    public void wakeup() {
        if (!enabled || consumer == null) {
            return;
        }
        consumer.wakeup();
    }

    @PreDestroy
    @Override
    public void close() {
        log.info("Closing ConsumerActionService...");
        if (enabled && consumer != null) {
            try {
                consumer.close();
                log.info("Kafka action consumer closed");
            } catch (Exception e) {
                log.error("Error closing Kafka action consumer", e);
            }
        }
    }
}