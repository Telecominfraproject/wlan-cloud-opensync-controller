package com.telecominfraproject.wlan.opensync.external.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;

/**
 * Listen for context started event so that we are register with routing service
 * 
 * @author yongli
 * 
 */
public class OpensyncGatewayControllerStartListener implements ApplicationListener<ContextStartedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayControllerStartListener.class);

    OpensyncCloudGatewayController controller;

    public OpensyncGatewayControllerStartListener(OpensyncCloudGatewayController controller) {
        this.controller = controller;
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        LOG.debug("Processing ContextStartedEvent event");
        controller.registerWithRoutingService();
    }
}
