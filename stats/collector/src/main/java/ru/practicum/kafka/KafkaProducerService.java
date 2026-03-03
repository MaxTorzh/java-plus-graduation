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

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaProducerService implements AutoCloseable {
    KafkaProducer<Long, SpecificRecordBase> producer;
    boolean enabled = true;

    public KafkaProducerService(
            @Value("${kafka.bootstrap-servers:}") String bootstrapServers,
            @Value("${kafka.enabled:true}") boolean enabled) {

        this.enabled = enabled && !bootstrapServers.isEmpty();

        if (this.enabled) {
            Properties config = new Properties();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());
            this.producer = new KafkaProducer<>(config);
        }
    }

    public void send(SpecificRecordBase action, Long eventId, Instant timestamp, String topic) {
        if (!enabled) {
            log.warn("Kafka producer disabled, message not sent: {}", action);
            return;
        }
        ProducerRecord<Long, SpecificRecordBase> record = new ProducerRecord<>(
                topic,
                null,
                timestamp.toEpochMilli(),
                eventId,
                action
        );
        producer.send(record);
        producer.flush();
    }

    @Override
    public void close() {
        if (enabled && producer != null) {
            producer.flush();
            producer.close(Duration.ofSeconds(5));
        }
    }
}