package com.autobook.orchestration.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessagePublisherImpl implements MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${queue.text-generation}")
    private String textGenerationQueue;

    @Value("${queue.image-generation}")
    private String imageGenerationQueue;

    @Value("${queue.pdf-assembly}")
    private String pdfAssemblyQueue;

    @Value("${queue.notification}")
    private String notificationQueue;

    @Override
    public void sendTextGenerationMessage(Object message) {
        rabbitTemplate.convertAndSend("autobook-exchange", "generation.text", message);
    }

    @Override
    public void sendImageGenerationMessage(Object message) {
        rabbitTemplate.convertAndSend("autobook-exchange", "generation.image", message);
    }

    @Override
    public void sendPdfAssemblyMessage(Object message) {
        rabbitTemplate.convertAndSend("autobook-exchange", "generation.pdf", message);
    }

    @Override
    public void sendNotificationMessage(Object message) {
        rabbitTemplate.convertAndSend("autobook-exchange", "notification", message);
    }
}