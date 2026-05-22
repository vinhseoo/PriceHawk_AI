package com.smartcart.messaging.config;

import com.smartcart.common.constants.MessageQueueConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean TopicExchange scrapeExchange() { return new TopicExchange(MessageQueueConstants.SCRAPE_EXCHANGE, true, false); }
    @Bean TopicExchange analysisExchange() { return new TopicExchange(MessageQueueConstants.ANALYSIS_EXCHANGE, true, false); }
    @Bean TopicExchange priceExchange() { return new TopicExchange(MessageQueueConstants.PRICE_EXCHANGE, true, false); }

    @Bean Queue scrapeRequestQueue() { return QueueBuilder.durable(MessageQueueConstants.SCRAPE_REQUEST_QUEUE).build(); }
    @Bean Queue productScrapedQueue() { return QueueBuilder.durable(MessageQueueConstants.PRODUCT_SCRAPED_QUEUE).build(); }
    @Bean Queue analysisRequestQueue() { return QueueBuilder.durable(MessageQueueConstants.ANALYSIS_REQUEST_QUEUE).build(); }
    @Bean Queue analysisCompletedQueue() { return QueueBuilder.durable(MessageQueueConstants.ANALYSIS_COMPLETED_QUEUE).build(); }
    @Bean Queue priceUpdatedQueue() { return QueueBuilder.durable(MessageQueueConstants.PRICE_UPDATED_QUEUE).build(); }
    @Bean Queue scrapeFailedQueue() { return QueueBuilder.durable(MessageQueueConstants.SCRAPE_FAILED_QUEUE).build(); }

    @Bean Binding scrapeRequestBinding() { return BindingBuilder.bind(scrapeRequestQueue()).to(scrapeExchange()).with(MessageQueueConstants.SCRAPE_REQUEST_KEY); }
    @Bean Binding productScrapedBinding() { return BindingBuilder.bind(productScrapedQueue()).to(scrapeExchange()).with(MessageQueueConstants.PRODUCT_SCRAPED_KEY); }
    @Bean Binding analysisRequestBinding() { return BindingBuilder.bind(analysisRequestQueue()).to(analysisExchange()).with(MessageQueueConstants.ANALYSIS_REQUEST_KEY); }
    @Bean Binding analysisCompletedBinding() { return BindingBuilder.bind(analysisCompletedQueue()).to(analysisExchange()).with(MessageQueueConstants.ANALYSIS_COMPLETED_KEY); }
    @Bean Binding priceUpdatedBinding() { return BindingBuilder.bind(priceUpdatedQueue()).to(priceExchange()).with(MessageQueueConstants.PRICE_UPDATED_KEY); }
}
