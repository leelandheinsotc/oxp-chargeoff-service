package com.olivertechnology.chargeoff.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
public class JmsPublisherImpl implements JmsPublisher{

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public JmsPublisherImpl(final JmsTemplate jmsTemplate,
                            final ObjectMapper objectMapper){
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(final String queueName,final String message) {
        jmsTemplate.convertAndSend(queueName,message);

    }

    @Override
    public void publishObject(final String queueName, final Object message) {

        try{
            jmsTemplate.convertAndSend(queueName,objectMapper.writeValueAsString(message));
        }catch(final IOException ex){
            log.error(ex.getLocalizedMessage(),ex);
        }

    }
}
