package ru.practicum.handler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.kafka.KafkaProducerService;
import jakarta.annotation.PostConstruct;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionHandler {
    final KafkaProducerService kafkaProducer;

    @Value("${kafka.topic:stats.user-actions.v1}")
    String topic;

    @Value("${kafka.enabled:true}")
    boolean kafkaEnabled;

    @PostConstruct
    void init() {
        log.info("UserActionHandler initialized, kafkaEnabled: {}, topic: {}", kafkaEnabled, topic);
    }

    public void handle(UserActionProto proto) {
        log.info("Processing user action: {}", proto);

        try {
            Instant timestamp = Instant.ofEpochSecond(proto.getTimestamp().getSeconds(), proto.getTimestamp().getNanos());
            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(proto.getUserId())
                    .setEventId(proto.getEventId())
                    .setActionType(getActionTypeAvro(proto.getActionType()))
                    .setTimestamp(timestamp)
                    .build();

            log.info("Sending to Kafka topic {}: {}", topic, avro);
            kafkaProducer.send(avro, proto.getEventId(), timestamp, topic);
            log.info("Event successfully sent to topic {}", topic);

        } catch (Exception e) {
            log.error("Failed to handle user action", e);
            throw new RuntimeException("Failed to handle user action", e);
        }
    }

    private ActionTypeAvro getActionTypeAvro(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }
}