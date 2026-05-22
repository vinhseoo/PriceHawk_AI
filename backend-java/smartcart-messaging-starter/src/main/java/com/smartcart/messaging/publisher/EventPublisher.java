package com.smartcart.messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(String exchange, String routingKey, Object event) {
        log.debug("Publishing to exchange={} routingKey={} payload={}", exchange, routingKey, event.getClass().getSimpleName());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
