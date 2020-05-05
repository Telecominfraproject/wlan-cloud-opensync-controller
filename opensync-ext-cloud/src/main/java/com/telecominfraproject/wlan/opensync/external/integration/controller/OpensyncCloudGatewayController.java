package com.telecominfraproject.wlan.opensync.external.integration.controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.core.server.container.ConnectorProperties;
import com.telecominfraproject.wlan.opensync.external.integration.ConnectusOvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;

/**
 * Opensync Gateway Controller - integration code for cloud deployment
 * 
 * @author yongli
 * @author dtop
 * 
 */
@RestController
@EnableScheduling
public class OpensyncCloudGatewayController {

	@Autowired
	ConnectorProperties connectorProperties;

	@Autowired
	private ServiceInstanceInformation serviceInstanceInfo;

	@Autowired
	private ConnectusOvsdbClientInterface connectusOvsdbClient;

	/**
	 * Flag indicates if this gateway has registered with routing service
	 */
	private boolean registeredWithRoutingService = false;

	private long registeredGwId = -1;

	/**
	 * Lock used to protected {@link #activeCustomerLock}
	 */
	private final ReadWriteLock activeCustomerLock = new ReentrantReadWriteLock();
	private final Lock activeCustomerReadLock = activeCustomerLock.readLock();
	private final Lock activeCustomerWriteLock = activeCustomerLock.writeLock();

	@Autowired
	private OvsdbSessionMapInterface ovsdbSessionMapInterface;

	/**
	 * Map <customerId, lastSeenTimestamp>
	 */
	private ConcurrentMap<Integer, Long> activeCustomerMap = new ConcurrentHashMap<>();

	/**
	 * latestTimetamp used when updating {@link #activeCustomerMap}
	 */
	private final BiFunction<Long, Long, Long> latestTimestamp = new BiFunction<Long, Long, Long>() {
		@Override
		public Long apply(Long oldValue, Long newValue) {
			if (newValue.compareTo(oldValue) > 0) {
				return newValue;
			}
			return oldValue;
		}
	};
	
    /**
     * Use connection internal hostname as the gateway name
     * 
     * @return
     */
    private String getGatewayName() {
        return connectorProperties.getInternalHostName();
    }

	private static final Logger LOG = LoggerFactory.getLogger(OpensyncCloudGatewayController.class);

	public void registerWithRoutingService() {
        synchronized (this) {
            if (registeredWithRoutingService) {
                return;
            }

//            CustomerEquipmentGwRecord gwRecord = new CustomerEquipmentGwRecord();
//            gwRecord.setDeploymentId(getDeploymentId());
//
//            // Internal facing service
//            gwRecord.setGatewayId(getGatewayName());
//
//            gwRecord.setIpAddr(connectorProperties.getInternalIpAddress().getHostAddress());
//            gwRecord.setPort(connectorProperties.getInternalPort());
            try {
//                CustomerEquipmentGwRecord result = this.eqRoutingSvc.registerGateway(gwRecord);
//                this.registeredGwId = result.getId();
//                LOG.info("Successfully registered (name={}, id={}) with Routing Service", result.getGatewayId(),
//                        registeredGwId);
                registeredWithRoutingService = true;
            } catch (RuntimeException e) {
                // failed
                LOG.error("Failed to register Customer Equipment Gateway (name={}) with Routing Service : {}",
                        getGatewayName(), e.getLocalizedMessage());
            }
        }
    }

	public void deregisterFromRoutingService() {
		// TODO Auto-generated method stub

	}

	public void updateActiveCustomer(int customerId) {
		this.activeCustomerReadLock.lock();
		try {
			this.activeCustomerMap.merge(customerId, System.currentTimeMillis(), latestTimestamp);
		} finally {
			this.activeCustomerReadLock.unlock();
		}

	}

}
