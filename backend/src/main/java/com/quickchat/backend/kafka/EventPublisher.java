package com.quickchat.backend.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * EventComponent의 발행(publish) 책임. 다른 컴포넌트는 KafkaTemplate을 직접 다루지 않는다
 * (aidlc-docs/inception/application-design/components.md).
 * 재시도 최대 3회는 application.yml의 spring.kafka.producer.retries 설정으로 처리한다
 * (nfr-design-patterns.md Q1 답변 A). 모든 재시도가 실패하면 실패 로그를 남긴다.
 */
@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka publish failed after retries. topic={}, key={}", topic, key, ex);
            }
        });
    }
}
