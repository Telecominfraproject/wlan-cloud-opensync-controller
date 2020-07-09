package com.telecominfraproject.wlan.opensync.experiment;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telecominfraproject.wlan.opensync.external.integration.OvsdbClientInterface;

@Profile("ovsdb_manager")
@RestController
@RequestMapping(value = "/api")
public class OpenSyncGatewayController {

	private static final Logger LOG = LoggerFactory.getLogger(OpenSyncGatewayController.class);

	@Autowired
	OvsdbClientInterface tipwlanOvsdbClient;

	@RequestMapping(value = "/connectedClients", method = RequestMethod.GET)
	public List<String> getConnectedClients() {
		List<String> ret = new ArrayList<String>(tipwlanOvsdbClient.getConnectedClientIds());
		LOG.info("Returning connected clients {}", ret);
		return ret;
	}

	@RequestMapping(value = "/changeRedirectorAddress", method = RequestMethod.POST)
	public String changeRedirectorAddress(@RequestParam String apId, @RequestParam String newRedirectorAddress) {
		LOG.info("Changing redirector address for AP {} to {}", apId, newRedirectorAddress);
		String ret = tipwlanOvsdbClient.changeRedirectorAddress(apId, newRedirectorAddress);
		LOG.info("Changed redirector address for AP {} to {}", apId, ret);
		return ret;
	}

	@RequestMapping(value = "/closeSession", method = RequestMethod.POST)
	public String closeSession(@RequestParam String apId) {
		LOG.info("Closing session for AP {} ", apId);
		String ret = tipwlanOvsdbClient.closeSession(apId);
		LOG.info("Closed session for AP {} ", apId);
		return ret;
	}

}
