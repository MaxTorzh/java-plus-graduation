package ru.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.KafkaConsumerService;
import ru.practicum.kafka.KafkaProducerService;
import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AggregationStarter {
    final KafkaConsumerService consumer;
    final KafkaProducerService producer;
    final UserActionService userActionService;

    @Value("${kafka.action-topic:user-actions}")
    String actionTopic;

    @Value("${kafka.similarity-topic:event-similarity}")
    String similarityTopic;

    @Value("${kafka.enabled:true}")
    boolean enabled;

    @PostConstruct
    public void init() {
        log.info("AggregationStarter initialized, enabled: {}", enabled);
        if (enabled) {
            new Thread(this::start).start();
        } else {
            log.info("Kafka disabled - aggregation not started");
        }
    }

    public void start() {
        if (!enabled) {
            log.info("Kafka disabled - start method ignored");
            return;
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            log.info("Подписка на топик {}", actionTopic);
            consumer.subscribe(List.of(actionTopic));

            while (true) {
                log.debug("Ожидание сообщений...");
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()) {
                    log.info("Получено {} сообщений", records.count());
                    for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                        try {
                            UserActionAvro action = (UserActionAvro) record.value();
                            log.info("Обработка действия пользователя = {}", action);
                            userActionService.updateSimilarity(action)
                                    .forEach(similarity -> producer.send(similarity, similarityTopic));
                            log.info("Событие от пользователя = {} обработано", action);
                        } catch (Exception e) {
                            log.error("Ошибка обработки записи", e);
                        }
                    }
                    log.debug("Выполнение фиксации смещений");
                    consumer.commitAsync();
                }
            }
        } catch (WakeupException e) {
            log.info("Получен WakeupException, останавливаемся");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от пользователей", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter");
        try {
            log.info("Сбрасывание всех данных в буфере");
            producer.flush();
            log.info("Фиксация смещений");
            consumer.commitAsync();
        } catch (Exception e) {
            log.error("Ошибка во время сброса данных", e);
        } finally {
            try {
                log.info("Закрываем консьюмер");
                consumer.close();
            } catch (Exception e) {
                log.error("Ошибка при закрытии consumer", e);
            }
            try {
                log.info("Закрываем продюсер");
                producer.close();
            } catch (Exception e) {
                log.error("Ошибка при закрытии producer", e);
            }
        }
    }
}