package com.telecominfraproject.wlan.opensync.external.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * Register for stop event so that we can de-register from routing service
 * 
 * @author yongli
 *
 */
@Component
public class OpensyncGatewayControllerStopListener implements ApplicationListener<ContextClosedEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayControllerStopListener.class);

	@Autowired
	private OpensyncCloudGatewayController controller;

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		LOG.debug("Processing ContextClosedEvent event");
		controller.deregisterFromRoutingService();
	}

}
