package ai.connectus.opensync.experiment;

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

import ai.connectus.opensync.ovsdb.ConnectusOvsdbClient;

@Profile("ovsdb_manager")
@RestController
public class OpenSyncExperimentController {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSyncExperimentController.class);
    
    @Autowired
    ConnectusOvsdbClient connectusOvsdbClient;
            
    @RequestMapping(value = "/connectedClients", method = RequestMethod.GET)
    public List<String> getConnectedClients() 
    {
        List<String> ret = new ArrayList<String>(connectusOvsdbClient.getConnectedClientIds());
        LOG.info("Returning connected clients {}", ret);
        return ret;
    }

    @RequestMapping(value = "/changeRedirectorAddress", method = RequestMethod.POST)
    public String changeRedirectorAddress(@RequestParam String apId, @RequestParam String newRedirectorAddress) 
    {        
        LOG.info("Changing redirector address for AP {} to {}", apId, newRedirectorAddress);
        String ret = connectusOvsdbClient.changeRedirectorAddress(apId, newRedirectorAddress);
        LOG.info("Changed redirector address for AP {} to {}", apId, ret);
        return ret;
    }

    
}
