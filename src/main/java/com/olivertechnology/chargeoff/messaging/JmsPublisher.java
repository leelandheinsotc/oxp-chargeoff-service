package com.olivertechnology.chargeoff.messaging;

public interface JmsPublisher {

    void publish(final String queueName,final String message);
    void publishObject(final String queueName,final Object message);
}
