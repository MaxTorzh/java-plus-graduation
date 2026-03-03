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
import ru.practicum.handler.UserActionHandler;
import ru.practicum.kafka.ConsumerActionService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionProcessor implements Runnable {
    final ConsumerActionService consumer;
    final UserActionHandler userActionHandler;

    @Value("${kafka.topics.action:user-actions}")
    String topic;

    @Value("${kafka.enabled:true}")
    boolean kafkaEnabled;

    @PostConstruct
    public void init() {
        log.info("UserActionProcessor initialized, kafkaEnabled: {}, topic: {}", kafkaEnabled, topic);
        if (kafkaEnabled) {
            new Thread(this).start();
        } else {
            log.info("Kafka disabled, processor not started");
        }
    }

    @Override
    public void run() {
        if (!kafkaEnabled) {
            log.info("Kafka disabled, run method ignored");
            return;
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            log.info("Подписка на топик {}", topic);
            consumer.subscribe(List.of(topic));

            while (true) {
                log.debug("Ожидание сообщений...");
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(Duration.ofMillis(5000));

                if (!records.isEmpty()) {
                    log.info("Получено {} сообщений", records.count());
                    for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                        try {
                            UserActionAvro avro = (UserActionAvro) record.value();
                            log.info("Обработка действия пользователя = {}", avro);
                            userActionHandler.handle(avro);
                            log.debug("Действие пользователя обработано");
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
            log.error("Ошибка во время обработки сообщений", e);
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        try {
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
        }
    }
}