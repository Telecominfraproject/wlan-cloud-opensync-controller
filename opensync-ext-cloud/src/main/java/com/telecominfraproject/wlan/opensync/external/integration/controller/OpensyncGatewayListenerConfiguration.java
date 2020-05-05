package com.telecominfraproject.wlan.opensync.external.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

@Component
public class OpensyncGatewayListenerConfiguration {
    @Autowired
    OpensyncCloudGatewayController controller;
    private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayControllerStartListener.class);

    @Bean
    public ApplicationListener<ContextClosedEvent> myStopEventListner() {
        LOG.debug("Creating stop event listener");
        return new OpensyncGatewayControllerStopListener(controller);
    }
    
    @Bean
    public ApplicationListener<ContextStartedEvent> myStartedEventListener() {
        LOG.debug("Creating start event listener");
        return new OpensyncGatewayControllerStartListener(controller);
    }
}
