package com.autobook.orchestration.service;

public interface MessagePublisher {
    void sendTextGenerationMessage(Object message);
    void sendImageGenerationMessage(Object message);
    void sendPdfAssemblyMessage(Object message);
    void sendNotificationMessage(Object message);
}