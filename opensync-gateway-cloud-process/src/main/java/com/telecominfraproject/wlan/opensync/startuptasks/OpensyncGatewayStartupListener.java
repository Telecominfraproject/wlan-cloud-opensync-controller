package com.telecominfraproject.wlan.opensync.startuptasks;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import com.telecominfraproject.wlan.core.model.pair.PairIntString;
import com.telecominfraproject.wlan.customer.service.CustomerServiceInterface;
import com.telecominfraproject.wlan.equipment.EquipmentServiceInterface;

@Configuration
public class OpensyncGatewayStartupListener implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncGatewayStartupListener.class);

    @Autowired
    private CustomerServiceInterface customerServiceInterface;

    @Autowired
    private EquipmentServiceInterface equipmentServiceInterface;

	@Override
	public void run(ApplicationArguments args) throws Exception {
        LOG.debug("OSGW startup commands");
        try {
//        	List<PairIntString> first10Customers = customerServiceInterface.getAll(10, -1);
//        	LOG.info("first10Customers: {}", first10Customers);
        	
        } catch (Exception e) {
            LOG.error("Got Exception ", e);
        }
        LOG.debug("Completed OSGW startup commands");

	}
}
