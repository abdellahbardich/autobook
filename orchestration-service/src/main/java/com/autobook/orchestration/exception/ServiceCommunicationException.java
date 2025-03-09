package com.autobook.orchestration.exception;

public class ServiceCommunicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ServiceCommunicationException(String message) {
        super(message);
    }

    public ServiceCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}