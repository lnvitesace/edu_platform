package com.edu.search.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ 消息队列配置。
 * <p>
 * 使用 Topic Exchange 实现课程事件的发布订阅模式。
 * 搜索服务通过单个队列绑定三种路由键（published/updated/deleted），
 * 将课程的全生命周期事件统一汇聚到同一个消费者处理，简化消费端逻辑。
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "course.exchange";
    public static final String QUEUE_SEARCH = "course.search.queue";
    public static final String ROUTING_KEY_PUBLISHED = "course.published";
    public static final String ROUTING_KEY_UPDATED = "course.updated";
    public static final String ROUTING_KEY_DELETED = "course.deleted";

    @Bean
    public TopicExchange courseExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue searchQueue() {
        return QueueBuilder.durable(QUEUE_SEARCH).build();
    }

    @Bean
    public Binding bindingPublished(Queue searchQueue, TopicExchange courseExchange) {
        return BindingBuilder.bind(searchQueue).to(courseExchange).with(ROUTING_KEY_PUBLISHED);
    }

    @Bean
    public Binding bindingUpdated(Queue searchQueue, TopicExchange courseExchange) {
        return BindingBuilder.bind(searchQueue).to(courseExchange).with(ROUTING_KEY_UPDATED);
    }

    @Bean
    public Binding bindingDeleted(Queue searchQueue, TopicExchange courseExchange) {
        return BindingBuilder.bind(searchQueue).to(courseExchange).with(ROUTING_KEY_DELETED);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }
}
