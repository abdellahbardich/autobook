package com.autobook.orchestration.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${queue.text-generation}")
    private String textGenerationQueue;

    @Value("${queue.image-generation}")
    private String imageGenerationQueue;

    @Value("${queue.pdf-assembly}")
    private String pdfAssemblyQueue;

    @Value("${queue.notification}")
    private String notificationQueue;

    @Bean
    public Queue textGenerationQueue() {
        return new Queue(textGenerationQueue, true);
    }

    @Bean
    public Queue imageGenerationQueue() {
        return new Queue(imageGenerationQueue, true);
    }

    @Bean
    public Queue pdfAssemblyQueue() {
        return new Queue(pdfAssemblyQueue, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(notificationQueue, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("autobook-exchange");
    }

    @Bean
    public Binding bindingTextGeneration(Queue textGenerationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(textGenerationQueue).to(exchange).with("generation.text");
    }

    @Bean
    public Binding bindingImageGeneration(Queue imageGenerationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(imageGenerationQueue).to(exchange).with("generation.image");
    }

    @Bean
    public Binding bindingPdfAssembly(Queue pdfAssemblyQueue, TopicExchange exchange) {
        return BindingBuilder.bind(pdfAssemblyQueue).to(exchange).with("generation.pdf");
    }

    @Bean
    public Binding bindingNotification(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with("notification");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}