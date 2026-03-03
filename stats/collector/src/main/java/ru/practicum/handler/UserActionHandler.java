package ru.practicum.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;
import ru.practicum.kafka.KafkaProducerService;
import java.time.Instant;

@Slf4j
@Component
public class UserActionHandler {

    private final KafkaProducerService kafkaProducer;
    private final String topic;

    @Autowired
    public UserActionHandler(
            KafkaProducerService kafkaProducer,
            @Value("${kafka.topic:user-actions}") String topic) {

        log.info("==================================================");
        log.info("UserActionHandler constructor called");
        log.info("kafkaProducer: {}", kafkaProducer != null ? "provided" : "null");
        log.info("topic: {}", topic);
        log.info("==================================================");

        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    @PostConstruct
    void init() {
        log.info("==================================================");
        log.info("UserActionHandler initialized successfully");
        log.info("kafkaProducer class: {}", kafkaProducer.getClass().getName());
        log.info("topic value: {}", topic);
        log.info("==================================================");
    }

    public void handle(UserActionProto proto) {
        log.info("Handling user action: userId={}, eventId={}, actionType={}",
                proto.getUserId(), proto.getEventId(), proto.getActionType());

        try {
            Instant timestamp = Instant.ofEpochSecond(
                    proto.getTimestamp().getSeconds(),
                    proto.getTimestamp().getNanos()
            );

            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(proto.getUserId())
                    .setEventId(proto.getEventId())
                    .setActionType(getActionTypeAvro(proto.getActionType()))
                    .setTimestamp(timestamp)
                    .build();

            log.debug("Sending to Kafka: {}", avro);
            kafkaProducer.send(avro, proto.getEventId(), timestamp, topic);
            log.info("Event successfully processed");

        } catch (Exception e) {
            log.error("Failed to handle user action", e);
            throw new RuntimeException("Failed to handle user action", e);
        }
    }

    private ActionTypeAvro getActionTypeAvro(ActionTypeProto actionType) {
        log.debug("Converting action type: {}", actionType);
        return switch (actionType) {
            case ACTION_VIEW -> {
                log.debug("Converting to VIEW");
                yield ActionTypeAvro.VIEW;
            }
            case ACTION_REGISTER -> {
                log.debug("Converting to REGISTER");
                yield ActionTypeAvro.REGISTER;
            }
            case ACTION_LIKE -> {
                log.debug("Converting to LIKE");
                yield ActionTypeAvro.LIKE;
            }
            default -> {
                log.error("Unknown action type: {}", actionType);
                throw new IllegalArgumentException("Unknown action type: " + actionType);
            }
        };
    }
}