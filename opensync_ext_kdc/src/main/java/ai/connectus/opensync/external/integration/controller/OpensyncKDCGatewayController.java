package ai.connectus.opensync.external.integration.controller;

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

import com.whizcontrol.core.model.json.BaseJsonModel;
import com.whizcontrol.core.model.service.CloudServiceType;
import com.whizcontrol.core.model.service.ServiceInstanceInformation;
import com.whizcontrol.core.server.container.ConnectorProperties;
import com.whizcontrol.customerequipmentgateway.models.CEGWBaseCommand;
import com.whizcontrol.customerequipmentgateway.models.CEGWCloseSessionRequest;
import com.whizcontrol.customerequipmentgateway.models.CEGWCommandResultCode;
import com.whizcontrol.customerequipmentgateway.models.CEGWConfigChangeNotification;
import com.whizcontrol.customerequipmentgateway.models.CEGWRouteCheck;
import com.whizcontrol.customerequipmentgateway.models.CustomerEquipmentCommand;
import com.whizcontrol.customerequipmentgateway.models.CustomerEquipmentCommandResponse;
import com.whizcontrol.customerequipmentgateway.models.GatewayDefaults;
import com.whizcontrol.equipmentrouting.EquipmentRoutingInterface;
import com.whizcontrol.equipmentroutinginfo.models.CustomerEquipmentGwRecord;
import com.whizcontrol.equipmentroutinginfo.models.EquipmentRoutingRecord;
import com.whizcontrol.equipmentroutinginfo.models.RoutingRegisterResponse;
import com.whizcontrol.server.exceptions.ConfigurationException;

import ai.connectus.opensync.external.integration.OvsdbSession;
import ai.connectus.opensync.external.integration.OvsdbSessionMapInterface;

/**
 * Opensync Gateway Controller - integration code for Kodacloud deployment
 * 
 * @author yongli
 * @author dtop
 * 
 */
@RestController
@EnableScheduling
public class OpensyncKDCGatewayController {

    @Autowired
    EquipmentRoutingInterface eqRoutingSvc;

    @Autowired
    ConnectorProperties connectorProperties;

    @Autowired
    private ServiceInstanceInformation serviceInstanceInfo;

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

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncKDCGatewayController.class);

    @RequestMapping(value = "/command", method = RequestMethod.POST)
    public CustomerEquipmentCommandResponse sendCommand(@RequestBody CEGWBaseCommand command) {
        LOG.debug("sendCommand({})", command);
        String qrCode = command.getEquipmentQRCode();
        if (BaseJsonModel.hasUnsupportedValue(command)) {
            LOG.error("[{}] Failed to deliver command {}, command contains unsupported value", qrCode, command);
            return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand.ordinal(),
                    "Unsupported value in command for " + qrCode);
        }
        OvsdbSession session = ovsdbSessionMapInterface.getSession(qrCode);
        if (session == null) {
            LOG.warn("[{}] Failed to deliver command {}, websocket session not found", qrCode, command);
            return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE.ordinal(),
                    "No session found for " + qrCode);
        }

        switch (command.getCommandType()) {
        case ConfigChangeNotification:
            return sendConfigChangeNotification(session, (CEGWConfigChangeNotification) command);
        case CloseSessionRequest:
            return closeSession(session, (CEGWCloseSessionRequest) command);
        case CheckRouting:
            return checkEquipmentRouting(session, (CEGWRouteCheck) command);
        case StartDebugEngine:
        case StopDebugEngine:
        case FirmwareDownloadRequest:
        case FirmwareFlashRequest:
        case RebootRequest:
        case BlinkRequest:
        case NeighbourhoodReport:
        case ClientDeauthRequest:
        case CellSizeRequest:
        case NewChannelRequest:
        case ReportCurrentAPCRequest:
        case FileUpdateRequest:
        case InterferenceThresholdUpdateRequest:
        case BestApConfigurationUpdateRequest:
        case StartPacketFileCapture:
        case StopPacketCapture:
        case ReportCurrentVLANRequest:
        case BSSIDToMacMonitoringRequest:
        case ChannelChangeAnnouncementRequest:
        case UploadLogFile:
        case TogglePoERequest:
        case RadioReset:
        case ClearScanTable:
        case WdsRequest:
        default:
            LOG.warn("[{}] Failed to deliver command {}, unsupported command type", qrCode, command);
            return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand.ordinal(),
                    "Invalid command type (" + command.getCommandType() + ") for equipment (" + qrCode + ")");
        }
    }

    @RequestMapping(value = "/defaults", method = RequestMethod.GET)
    public GatewayDefaults retrieveGatewayDefaults() 
    {
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
    private CustomerEquipmentCommandResponse checkEquipmentRouting(OvsdbSession session, CEGWRouteCheck command) {
        if (null != command.getRoutingId()) {
            if (!command.getRoutingId().equals(session.getRoutingId())) {

                LOG.info("[C:{} E:{} R:{}] Stale routing entry ({}) detected",
                        session.getCustomerId(), command.getEquipmentQRCode(),
                        session.getRoutingId(), command.getRoutingId());

                return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE.ordinal(),
                        "Inactive Route Identifer");
            }
        }
        return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.Success.ordinal(), "Route active");
    }

    private CustomerEquipmentCommandResponse sendConfigChangeNotification(OvsdbSession session,
            CEGWConfigChangeNotification command) {
        
        return sendMessage(session, command.getEquipmentQRCode(), command);
    }

    private CustomerEquipmentCommandResponse closeSession(OvsdbSession session, CEGWCloseSessionRequest command) {
        try {
            session.getOvsdbClient().shutdown();
        } catch (Exception e) {
            LOG.error("[{}] Failed to close session on CE: {}", command.getEquipmentQRCode(), e.getLocalizedMessage());
            return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.FailedToSend.ordinal(),
                    "Failed to send command " + command.getCommandType() + " to " + command.getEquipmentQRCode() + ": "
                            + e.getMessage());
        }
        LOG.debug("[{}] Closed session to CE", command.getEquipmentQRCode());
        return new CustomerEquipmentCommandResponse(CEGWCommandResultCode.Success.ordinal(),
                "Closed session to " + command.getEquipmentQRCode());

    }

    /**
     * Deliver a message in payload to the CE
     * 
     * @param session
     * @param qrCode
     * @param command
     * @param request
     * @return
     */
    private CustomerEquipmentCommandResponse sendMessage(OvsdbSession session, String qrCode,
            CustomerEquipmentCommand command) {

        LOG.debug("Queued command {} for {}", command.getCommandType(), qrCode);
        CustomerEquipmentCommandResponse response = new CustomerEquipmentCommandResponse(CEGWCommandResultCode.Success.ordinal(),
                "Queued Command " + command.getCommandType() + " to " + qrCode);
        
        //TODO: implement sending command via ovsdb session.
        
        response.setQueued(true);
        return response;
    }

    @RequestMapping(value = "/commandWithUser", method = RequestMethod.POST)
    public CustomerEquipmentCommandResponse sendCommandWithAuthUser(@RequestBody CustomerEquipmentCommand command,
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

        return sendCommand(command);
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
                        "Unabled to register gateway with routing service: routing service interface not initialized");
            }
            CustomerEquipmentGwRecord gwRecord = new CustomerEquipmentGwRecord(CloudServiceType.CEGW, getDeploymentId());

            // Internal facing service
            gwRecord.setGatewayId(getGatewayName());
            gwRecord.setIpAddr(connectorProperties.getInternalIpAddress().getHostAddress());
            gwRecord.setPort(connectorProperties.getInternalPort());

            try {
                CustomerEquipmentGwRecord result = this.eqRoutingSvc.registerGateway(gwRecord);
                this.registeredGwId = result.getId();
                LOG.info("Successfully registered (name={}, id={}) with Routing Service", result.getGatewayId(),
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
                eqRoutingSvc.deregisterGateway(registeredGwId);
                LOG.info("Deregistered Customer Equipment Gateway (name={},id={}) with Routing Service",
                        getGatewayName(), this.registeredGwId);
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
    public RoutingRegisterResponse registerCustomerEquipment(String equipmentName, Integer customerId,
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
        routingRecord.setGatewayRecordId(this.registeredGwId);
        try {
            RoutingRegisterResponse result = eqRoutingSvc.registerUERouteForDeployment(routingRecord,
                    getDeploymentId());
            LOG.debug("Registered customer equipment (name={},id={}) with route id={}", equipmentName, equipmentId,
                    result.getRoutingRecord().getId());
            return result;

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

            eqRoutingSvc.deregisterUERoute(routingId);
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
                this.eqRoutingSvc.updateActiveCustomer(activeMap, getDeploymentId());
            }
        } catch (RuntimeException exp) {
            LOG.error("Failed to update active customer records due to exception {}", exp.getLocalizedMessage());
        }
    }


    /**
     * Use connection internal hostname as the gateway name
     * 
     * @return
     */
    private String getGatewayName() {
        return connectorProperties.getInternalHostName();
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
