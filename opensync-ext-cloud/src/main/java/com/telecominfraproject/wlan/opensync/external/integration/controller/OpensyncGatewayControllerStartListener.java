package com.telecominfraproject.wlan.opensync.external.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;

/**
 * Listen for context started event so that we are register with routing service
 * 
 * @author yongli
 * 
 */
@Component
public class OpensyncGatewayControllerStartListener implements ApplicationListener<ContextStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayControllerStartListener.class);

    @Autowired
    private OpensyncCloudGatewayController controller;

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        LOG.debug("Processing ContextStartedEvent event");
        controller.registerWithRoutingService();
    }
}
