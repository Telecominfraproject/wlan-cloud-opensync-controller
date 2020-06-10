package com.telecominfraproject.wlan.opensync.external.integration.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.core.server.container.ConnectorProperties;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWBaseCommand;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWBlinkRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWCloseSessionRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWCommandResultCode;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWConfigChangeNotification;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWRouteCheck;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWStartDebugEngine;
import com.telecominfraproject.wlan.equipmentgateway.models.EquipmentCommand;
import com.telecominfraproject.wlan.equipmentgateway.models.EquipmentCommandResponse;
import com.telecominfraproject.wlan.equipmentgateway.models.GatewayDefaults;
import com.telecominfraproject.wlan.opensync.external.integration.ConnectusOvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentGatewayRecord;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;

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

	
    public static class ListOfEquipmentCommandResponses extends ArrayList<EquipmentCommandResponse> {
        private static final long serialVersionUID = 3070319062835500930L;
    }

	@Autowired
	RoutingServiceInterface eqRoutingSvc;

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
	
	private EquipmentGatewayRecord registeredGateway;

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

	private static final Logger LOG = LoggerFactory.getLogger(OpensyncCloudGatewayController.class);

	@RequestMapping(value = "/commands", method = RequestMethod.POST)
	public ListOfEquipmentCommandResponses sendCommands(@RequestBody List<CEGWBaseCommand> commands) {
		ListOfEquipmentCommandResponses ret = new ListOfEquipmentCommandResponses(); 
		if(commands == null) {
			return ret;
		}
		
		commands.forEach(command -> {
			LOG.debug("sendCommands - processing {}", command);

			String inventoryId = command.getInventoryId();

			if (com.telecominfraproject.wlan.core.model.json.BaseJsonModel.hasUnsupportedValue(command)) {
				LOG.error("[{}] Failed to deliver command {}, command contains unsupported value", inventoryId, command);
				ret.add( new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand,
						"Unsupported value in command for " + inventoryId, command, registeredGateway.getHostname(), registeredGateway.getPort()) );
				return;
			}
			OvsdbSession session = ovsdbSessionMapInterface.getSession(inventoryId);
			if (session == null) {
				LOG.warn("[{}] Failed to deliver command {}, equipment session not found", inventoryId, command);
				ret.add( new EquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE,
						"No session found for " + inventoryId, command, registeredGateway.getHostname(), registeredGateway.getPort()) );
				return;
			}
	
			switch (command.getCommandType()) {
	
			case ConfigChangeNotification:
				ret.add( sendConfigChangeNotification(session, (CEGWConfigChangeNotification) command) );
				break;
			case CloseSessionRequest:
				ret.add( closeSession(session, (CEGWCloseSessionRequest) command) );
				break;
			case CheckRouting:
				ret.add( checkEquipmentRouting(session, (CEGWRouteCheck) command) );
				break;
			case BlinkRequest:
				ret.add( processBlinkRequest(session, (CEGWBlinkRequest) command) );
				break;
			case StartDebugEngine:
				ret.add ( processChangeRedirector(session, (CEGWStartDebugEngine) command) );
				break;
	
			default:
				LOG.warn("[{}] Failed to deliver command {}, unsupported command type", inventoryId, command);
				ret.add ( new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand,
						"Invalid command type (" + command.getCommandType() + ") for equipment (" + inventoryId + ")", 
						command, registeredGateway.getHostname(), registeredGateway.getPort()) );
			}
		
		});
		
		return ret;
	}

	@RequestMapping(value = "/defaults", method = RequestMethod.GET)
	public GatewayDefaults retrieveGatewayDefaults() {
		return new GatewayDefaults();
	}

	/**
	 * Verify a route to customer equipment
	 * 
	 * @param session
	 * @param command
	 * @param protocolVersion
	 * @return NoRouteToCE if route Id does not match or Success
	 */
	private EquipmentCommandResponse checkEquipmentRouting(OvsdbSession session, CEGWRouteCheck command) {
		if (null != command.getRoutingId()) {
			if (!command.getRoutingId().equals(session.getRoutingId())) {

				LOG.info("[C:{} E:{} R:{}] Stale routing entry ({}) detected", session.getCustomerId(),
						command.getInventoryId(), session.getRoutingId(), command.getRoutingId());

				return new EquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE,
						"Inactive Route Identifer", command, 
						registeredGateway.getHostname(), registeredGateway.getPort());
			}
		}
		return new EquipmentCommandResponse(CEGWCommandResultCode.Success, "Route active", 
				command, registeredGateway.getHostname(), registeredGateway.getPort());
	}

	private EquipmentCommandResponse sendConfigChangeNotification(OvsdbSession session,
			CEGWConfigChangeNotification command) {

		return sendMessage(session, command.getInventoryId(), command);
	}

	private EquipmentCommandResponse closeSession(OvsdbSession session, CEGWCloseSessionRequest command) {
		try {
			session.getOvsdbClient().shutdown();
		} catch (Exception e) {
			LOG.error("[{}] Failed to close session on CE: {}", command.getInventoryId(), e.getLocalizedMessage());
			return new EquipmentCommandResponse(CEGWCommandResultCode.FailedToSend,
					"Failed to send command " + command.getCommandType() + " to " + command.getInventoryId() + ": "
							+ e.getMessage(), command, 
							registeredGateway.getHostname(), registeredGateway.getPort());
		}
		LOG.debug("[{}] Closed session to CE", command.getInventoryId());
		return new EquipmentCommandResponse(CEGWCommandResultCode.Success,
				"Closed session to " + command.getInventoryId(), command, 
				registeredGateway.getHostname(), registeredGateway.getPort());

	}

	/**
	 * Deliver a message in payload to the CE
	 * 
	 * @param session
	 * @param inventoryId
	 * @param command
	 * @param request
	 * @return
	 */
	private EquipmentCommandResponse sendMessage(OvsdbSession session, String inventoryId,
			EquipmentCommand command) {

		LOG.debug("Received command {} for {}", command.getCommandType(), inventoryId);
		EquipmentCommandResponse response = new EquipmentCommandResponse(
				CEGWCommandResultCode.Success,
				"Received Command " + command.getCommandType() + " for " + inventoryId, command, 
				registeredGateway.getHostname(), registeredGateway.getPort());

		if (command instanceof CEGWConfigChangeNotification) {
			connectusOvsdbClient.processConfigChanged(inventoryId);
		} else if (command instanceof CEGWStartDebugEngine) {
			// dtop: we will be using CEGWStartDebugEngine command to deliver request to
			// change redirector
			// TODO: after the demo introduce a specialized command for this!
			String newRedirectorAddress = ((CEGWStartDebugEngine) command).getGatewayHostname();
			connectusOvsdbClient.changeRedirectorAddress(inventoryId, newRedirectorAddress);
		}

		return response;
	}

	private EquipmentCommandResponse processChangeRedirector(OvsdbSession session,
			CEGWStartDebugEngine command) {
		return sendMessage(session, command.getInventoryId(), command);
	}

	private EquipmentCommandResponse processBlinkRequest(OvsdbSession session, CEGWBlinkRequest command) {

		return sendMessage(session, command.getInventoryId(), command);
	}

	@RequestMapping(value = "/commandWithUser", method = RequestMethod.POST)
	public EquipmentCommandResponse sendCommandWithAuthUser(@RequestBody EquipmentCommand command,
			@AuthenticationPrincipal Object requestUser, HttpServletRequest httpServletRequest) {

		// use these properties to get address and port where request has
		// arrived
		httpServletRequest.getLocalAddr();
		httpServletRequest.getLocalPort();

		// requestUser will be instance of
		// org.springframework.security.core.userdetails.User for client auth
		// and digest auth,
		// although other auth providers may return something entirely different
		if (requestUser instanceof User) {
			LOG.debug("calling command with auth principal: {}", ((User) requestUser).getUsername());
		} else {
			LOG.debug("calling command with auth principal: {}", requestUser);
		}

		// This is a test method to show how to get access to the auth user
		// object for a given request

		return sendCommands(Arrays.asList(command)).get(0);
	}

	/**
	 * Register this controller with Equipment Routing Service
	 */
	public void registerWithRoutingService() {
		synchronized (this) {
			if (registeredWithRoutingService) {
				return;
			}

			if (eqRoutingSvc == null) {
				throw new ConfigurationException(
						"Unable to register gateway with routing service: routing service interface not initialized");
			}
			EquipmentGatewayRecord gwRecord = new EquipmentGatewayRecord();

			// external facing service, protected by the client certificate auth
			gwRecord.setHostname(getGatewayName());
			gwRecord.setIpAddr(connectorProperties.getExternalIpAddress().getHostAddress());
			gwRecord.setPort(connectorProperties.getExternalPort());

			try {

				EquipmentGatewayRecord result = this.eqRoutingSvc.registerGateway(gwRecord);
				this.registeredGwId = result.getId();
				this.registeredGateway = result;
				LOG.info("Successfully registered (name={}, id={}) with Routing Service", result.getHostname(),
						registeredGwId);
				registeredWithRoutingService = true;
			} catch (RuntimeException e) {
				// failed
				LOG.error("Failed to register Customer Equipment Gateway (name={}) with Routing Service : {}",
						getGatewayName(), e.getLocalizedMessage());
			}
		}
	}

	/**
	 * Return the current deployment identifier
	 * 
	 * @return
	 */
	public int getDeploymentId() {
		return serviceInstanceInfo.getDeploymentId();
	}

	/**
	 * De-register from Routing service
	 */
	public void deregisterFromRoutingService() {
		if (registeredWithRoutingService) {
			try {
				eqRoutingSvc.deleteGateway(registeredGwId);
				LOG.info("Deregistered Customer Equipment Gateway (name={},id={}) with Routing Service",
						getGatewayName(), this.registeredGwId);
				this.registeredGwId = -1;
				this.registeredGateway = null;
			} catch (Exception e) {
				// failed
				LOG.error("Failed to deregister Customer Equipment Gateway (name={},id={}) with Routing Service: {}",
						getGatewayName(), this.registeredGwId, e.getLocalizedMessage());
			}
			registeredWithRoutingService = false;
		}
	}

	public long getRegisteredGwId() {
		return this.registeredGwId;
	}

	/**
	 * Register a customer equipment with this gateway
	 * 
	 * @param equipmentName
	 * @param customerId
	 * @param equipmentId
	 * @return associationId
	 */
	public EquipmentRoutingRecord registerCustomerEquipment(String equipmentName, Integer customerId,
			Long equipmentId) {
		registerWithRoutingService();
		if (!registeredWithRoutingService) {
			LOG.error("Unable to register customer equipement (name={},id={}): gateway not registered.", equipmentName,
					equipmentId);
			return null;
		}
		EquipmentRoutingRecord routingRecord = new EquipmentRoutingRecord();
		routingRecord.setCustomerId(customerId);
		routingRecord.setEquipmentId(equipmentId);
		routingRecord.setGatewayId(this.registeredGwId);
		try {
			routingRecord = eqRoutingSvc.create(routingRecord);

			LOG.debug("Registered customer equipment (name={},id={}) with route id={}", equipmentName, equipmentId,
					routingRecord.getId());
			return routingRecord;

		} catch (Exception e) {
			LOG.error("Failed to register customer equipement (name={},id={}): {}", equipmentName, equipmentId,
					e.getLocalizedMessage());
		}
		return null;
	}

	public void deregisterCustomerEquipment(Long routingId, String equipmentName, Long equipmentId) {
		if (!registeredWithRoutingService) {
			LOG.error("Unable to deregister customer equipement (name={},id={}): gateway not registered", equipmentName,
					equipmentId);
			return;
		}
		try {
			LOG.debug("Deregistering customer equipment (name={},id={}) with route id={}", equipmentName, equipmentId,
					routingId);

			eqRoutingSvc.delete(routingId);
		} catch (Exception e) {
			LOG.error("Failed to deregister customer equipement (name={},id={}) with route id={}: {}", equipmentName,
					equipmentId, routingId, e.getLocalizedMessage());
		}
	}

	/**
	 * Run every 5 minutes
	 */
	@Scheduled(initialDelay = 5 * 60 * 1000, fixedRate = 5 * 60 * 1000)
	public void updateActiveCustomer() {
		try {
			Map<Integer, Long> activeMap = this.getActiveCustomerMapForUpdate();
			if (null != activeMap) {
				LOG.info("Updating active customer records, total record size {}", activeMap.size());
//				this.eqRoutingSvc.updateActiveCustomer(activeMap, getDeploymentId());
			}
		} catch (RuntimeException exp) {
			LOG.error("Failed to update active customer records due to exception {}", exp.getLocalizedMessage());
		}
	}

	/**
	 * Use connection external hostname as the gateway name
	 * 
	 * @return
	 */
	private String getGatewayName() {
		return connectorProperties.getExternalHostName();
	}

	/**
	 * Update the active timestamp for the customer
	 * 
	 * @param customerId
	 */
	public void updateActiveCustomer(int customerId) {
		this.activeCustomerReadLock.lock();
		try {
			this.activeCustomerMap.merge(customerId, System.currentTimeMillis(), latestTimestamp);
		} finally {
			this.activeCustomerReadLock.unlock();
		}
	}

	/**
	 * Swap the active customer map for reporting if it contains records.
	 * 
	 * @return null if no records.
	 */
	protected Map<Integer, Long> getActiveCustomerMapForUpdate() {
		this.activeCustomerWriteLock.lock();
		try {
			Map<Integer, Long> map = null;
			if (!this.activeCustomerMap.isEmpty()) {
				map = this.activeCustomerMap;
				this.activeCustomerMap = new ConcurrentHashMap<>();
			}

			return map;
		} finally {
			this.activeCustomerWriteLock.unlock();
		}
	}
}
