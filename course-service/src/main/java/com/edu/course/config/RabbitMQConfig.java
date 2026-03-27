package com.edu.course.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * RabbitMQ 消息队列配置。
 *
 * 课程事件通过消息队列异步通知其他服务（如搜索服务索引更新）。
 * 使用 Topic Exchange 支持灵活的路由规则，便于未来扩展更多消费者。
 *
 * 消息流向：CourseService -> course.exchange -> course.search.queue -> SearchService
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "course.exchange";
    public static final String QUEUE_SEARCH = "course.search.queue";

    // Routing Key 定义：搜索服务监听所有课程变更事件
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

    /**
     * 使用 JSON 消息转换器，便于跨语言消费者解析消息体。
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new JacksonJsonMessageConverter((JsonMapper) objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
