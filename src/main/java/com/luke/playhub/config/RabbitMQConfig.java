package com.luke.playhub.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luke
 * @since 2025/11/21 21:10
 */

@Configuration
public class RabbitMQConfig {

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true); //给消息设置id
        return converter;
    }

    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange("seckill.exchange", true, false);
    }

    @Bean
    public Queue seckillQueue() {
        return new Queue("seckill.order.queue", true, false, false);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with("seckill.order");
    }
}
