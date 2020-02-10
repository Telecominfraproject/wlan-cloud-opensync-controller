package ai.connectus.opensync.external.integration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Register for stop event so that we can de-register from routing service
 * 
 * @author yongli
 *
 */
public class OpensyncGatewayControllerStopListener implements ApplicationListener<ContextClosedEvent> {
    OpensyncKDCGatewayController controller;

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayControllerStopListener.class);
    
    public OpensyncGatewayControllerStopListener(OpensyncKDCGatewayController controller) {
        this.controller = controller;
    }
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.debug("Processing ContextClosedEvent event");
        controller.deregisterFromRoutingService();
    }

}
