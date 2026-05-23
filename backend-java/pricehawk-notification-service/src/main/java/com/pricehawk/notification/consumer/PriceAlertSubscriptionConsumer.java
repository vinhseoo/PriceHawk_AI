package com.pricehawk.notification.consumer;

import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.PriceAlertSubscriptionEvent;
import com.pricehawk.notification.service.PriceAlertSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes PriceAlertSubscriptionEvent from User Service.
 * Maintains the Redis-backed subscription store in PriceAlertSubscriptionService.
 *
 * event.active == true  → subscribe / update target price
 * event.active == false → unsubscribe
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertSubscriptionConsumer {

    private final PriceAlertSubscriptionService subscriptionService;

    @RabbitListener(queues = MessageQueueConstants.PRICE_ALERT_SUB_QUEUE)
    public void onSubscriptionEvent(PriceAlertSubscriptionEvent event) {
        log.debug("Subscription event: userId={} productId={} active={} targetPrice={}",
                event.getUserId(), event.getProductId(), event.isActive(), event.getTargetPrice());

        if (event.isActive()) {
            subscriptionService.subscribe(
                    event.getUserId(),
                    event.getUserEmail(),
                    event.getProductId(),
                    event.getTargetPrice()
            );
        } else {
            subscriptionService.unsubscribe(event.getUserId(), event.getProductId());
        }
    }
}
