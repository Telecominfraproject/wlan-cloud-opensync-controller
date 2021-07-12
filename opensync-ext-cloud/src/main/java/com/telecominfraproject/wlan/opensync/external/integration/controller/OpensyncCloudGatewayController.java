
package com.telecominfraproject.wlan.opensync.external.integration.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

import com.telecominfraproject.wlan.core.client.PingClient;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.service.GatewayType;
import com.telecominfraproject.wlan.core.model.service.ServiceInstanceInformation;
import com.telecominfraproject.wlan.core.server.container.ConnectorProperties;
import com.telecominfraproject.wlan.datastore.exceptions.DsEntityNotFoundException;
import com.telecominfraproject.wlan.equipment.models.CellSizeAttributes;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWBaseCommand;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWBlinkRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWChangeRedirectorHost;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWClientBlocklistChangeNotification;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWCloseSessionRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWCommandResultCode;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWConfigChangeNotification;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWFirmwareDownloadRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWFirmwareFlashRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWNewChannelRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWCellSizeAttributesRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWRadioResetRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWRebootRequest;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWRouteCheck;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWStartDebugEngine;
import com.telecominfraproject.wlan.equipmentgateway.models.CEGWStopDebugEngine;
import com.telecominfraproject.wlan.equipmentgateway.models.EquipmentCommand;
import com.telecominfraproject.wlan.equipmentgateway.models.EquipmentCommandResponse;
import com.telecominfraproject.wlan.equipmentgateway.models.GatewayDefaults;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbClientInterface;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSession;
import com.telecominfraproject.wlan.opensync.external.integration.OvsdbSessionMapInterface;
import com.telecominfraproject.wlan.routing.RoutingServiceInterface;
import com.telecominfraproject.wlan.routing.models.EquipmentGatewayRecord;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;
import com.telecominfraproject.wlan.server.exceptions.ConfigurationException;
import com.telecominfraproject.wlan.status.equipment.models.EquipmentResetMethod;

/**
 * Opensync Gateway Controller - integration code for cloud deployment
 *
 * @author yongli
 * @author dtop
 *
 */
@RestController
@EnableScheduling
@RequestMapping(value = "/api")
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
    private OvsdbClientInterface tipwlanOvsdbClient;

    @Autowired
    private PingClient pingClient;

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
    private final BiFunction<Long, Long, Long> latestTimestamp = new BiFunction<>() {

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
        if (commands == null) {
            return ret;
        }

        commands.stream().forEach(new Consumer<CEGWBaseCommand>() {

            @Override
            public void accept(CEGWBaseCommand command) {
                LOG.debug("sendCommands - processing {}", command);

                String inventoryId = command.getInventoryId();

                if (com.telecominfraproject.wlan.core.model.json.BaseJsonModel.hasUnsupportedValue(command)) {
                    LOG.error("[{}] Failed to deliver command {}, command contains unsupported value", inventoryId, command);
                    ret.add(new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand, "Unsupported value in command for " + inventoryId, command,
                            registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort()));
                    return;
                }
                OvsdbSession session = ovsdbSessionMapInterface.getSession(inventoryId);
                if (session == null) {
                    LOG.warn("[{}] Failed to deliver command {}, equipment session not found", inventoryId, command);
                    ret.add(new EquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE, "No session found for " + inventoryId, command,
                            registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort()));
                    return;
                }

                switch (command.getCommandType()) {

                    case ConfigChangeNotification:
                        ret.add(sendConfigChangeNotification(session, (CEGWConfigChangeNotification) command));
                        break;
                    case CloseSessionRequest:
                        ret.add(closeSession(session, (CEGWCloseSessionRequest) command));
                        break;
                    case CheckRouting:
                        ret.add(checkEquipmentRouting(session, (CEGWRouteCheck) command));
                        break;
                    case BlinkRequest:
                        ret.add(processBlinkRequest(session, (CEGWBlinkRequest) command));
                        break;
                    case ChangeRedirectorHost:
                        ret.add(processChangeRedirector(session, (CEGWChangeRedirectorHost) command));
                        break;
                    case StartDebugEngine:
                        ret.add(processStartDebugEngine(session, (CEGWStartDebugEngine) command));
                        break;
                    case StopDebugEngine:
                        ret.add(processStopDebugEngine(session, (CEGWStopDebugEngine) command));
                        break;
                    case FirmwareDownloadRequest:
                        ret.add(processFirmwareDownload(session, (CEGWFirmwareDownloadRequest) command));
                        break;
                    case FirmwareFlashRequest:
                        ret.add(processFirmwareFlash(session, (CEGWFirmwareFlashRequest) command));
                        break;
                    case RadioReset:
                        ret.add(processRadioReset(session, (CEGWRadioResetRequest) command));
                        break;
                    case RebootRequest:
                        ret.add(processRadioReboot(session, (CEGWRebootRequest) command));
                        break;
                    case ClientBlocklistChangeNotification:
                        ret.add(sendClientBlocklistChangeNotification(session, (CEGWClientBlocklistChangeNotification) command));
                        break;
                    case NewChannelRequest:
                        ret.add(sendNewChannelRequest(session, (CEGWNewChannelRequest) command));
                        break;
                    case CellSizeAttributesRequest:
                        ret.add(sendCellSizeRequest(session, (CEGWCellSizeAttributesRequest) command));
                        break;
                    default:
                        LOG.warn("[{}] Failed to deliver command {}, unsupported command type", inventoryId, command);
                        ret.add(new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand,
                                "Invalid command type (" + command.getCommandType() + ") for equipment (" + inventoryId + ")", command,
                                registeredGateway == null ? null : registeredGateway.getHostname(),
                                registeredGateway == null ? -1 : registeredGateway.getPort()));
                }

            }

        });

        return ret;
    }

    private EquipmentCommandResponse processFirmwareDownload(OvsdbSession session, CEGWFirmwareDownloadRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processFirmwareFlash(OvsdbSession session, CEGWFirmwareFlashRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processRadioReset(OvsdbSession session, CEGWRadioResetRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processRadioReboot(OvsdbSession session, CEGWRebootRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
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
     * @return NoRouteToCE if route Id does not match or Success
     */
    private EquipmentCommandResponse checkEquipmentRouting(OvsdbSession session, CEGWRouteCheck command) {
        if (null != command.getRoutingId()) {
            if (!command.getRoutingId().equals(session.getRoutingId())) {

                LOG.info("[E:{} R:{}] Stale routing entry ({}) detected", command.getInventoryId(), session.getRoutingId(),
                        command.getRoutingId());

                return new EquipmentCommandResponse(CEGWCommandResultCode.NoRouteToCE, "Inactive Route Identifer", command,
                        registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());
            }
        }
        return new EquipmentCommandResponse(CEGWCommandResultCode.Success, "Route active", command,
                registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());
    }

    private EquipmentCommandResponse sendConfigChangeNotification(OvsdbSession session, CEGWConfigChangeNotification command) {

        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse closeSession(OvsdbSession session, CEGWCloseSessionRequest command) {
        try {
            session.getOvsdbClient().shutdown();
        } catch (Exception e) {
            LOG.error("[{}] Failed to close session on CE: {}", command.getInventoryId(), e.getLocalizedMessage());
            return new EquipmentCommandResponse(CEGWCommandResultCode.FailedToSend,
                    "Failed to send command " + command.getCommandType() + " to " + command.getInventoryId() + ": " + e.getMessage(), command,
                    registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());
        }
        LOG.debug("[{}] Closed session to CE", command.getInventoryId());
        return new EquipmentCommandResponse(CEGWCommandResultCode.Success, "Closed session to " + command.getInventoryId(), command,
                registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());

    }

    private EquipmentCommandResponse sendClientBlocklistChangeNotification(OvsdbSession session, CEGWClientBlocklistChangeNotification command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse sendNewChannelRequest(OvsdbSession session, CEGWNewChannelRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse sendCellSizeRequest(OvsdbSession session, CEGWCellSizeAttributesRequest command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    /**
     * Deliver a message in payload to the CE
     *
     * @param session
     * @param inventoryId
     * @param command
     * @return
     */
    private EquipmentCommandResponse sendMessage(OvsdbSession session, String inventoryId, EquipmentCommand command) {

        LOG.debug("Received command {} for {}", command.getCommandType(), inventoryId);
        EquipmentCommandResponse response =
                new EquipmentCommandResponse(CEGWCommandResultCode.Success, "Received Command " + command.getCommandType() + " for " + inventoryId, command,
                        registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());

        if (command instanceof CEGWBlinkRequest) {
            String resultDetails = tipwlanOvsdbClient.processBlinkRequest(inventoryId, ((CEGWBlinkRequest)command).getBlinkAllLEDs()); 
            response.setResultDetail(resultDetails);
        } else if (command instanceof CEGWConfigChangeNotification) {
            tipwlanOvsdbClient.processConfigChanged(inventoryId);
        } else if (command instanceof CEGWClientBlocklistChangeNotification) {
            tipwlanOvsdbClient.processClientBlocklistChange(inventoryId, ((CEGWClientBlocklistChangeNotification) command).getBlockList());
        } else if (command instanceof CEGWChangeRedirectorHost) {
            String newRedirectorAddress = ((CEGWChangeRedirectorHost) command).getRedirectorHost();
            tipwlanOvsdbClient.changeRedirectorHost(inventoryId, newRedirectorAddress);
        } else if (command instanceof CEGWStartDebugEngine) {
            String gatewayHostname = ((CEGWStartDebugEngine) command).getGatewayHostname();
            int gatewayPort = ((CEGWStartDebugEngine) command).getGatewayPort();
            tipwlanOvsdbClient.startDebugEngine(inventoryId, gatewayHostname, gatewayPort);
        } else if (command instanceof CEGWStopDebugEngine) {
            tipwlanOvsdbClient.stopDebugEngine(inventoryId);
        } else if (command instanceof CEGWNewChannelRequest) {
            CEGWNewChannelRequest request = (CEGWNewChannelRequest) command;
            Map<RadioType, Integer> newBackupChannels = request.getNewBackupChannels();
            Map<RadioType, Integer> newPrimaryChannels = request.getNewPrimaryChannels();

            String resultDetails = tipwlanOvsdbClient.processNewChannelsRequest(inventoryId, newBackupChannels, newPrimaryChannels);
            response.setResultDetail(resultDetails);
        } else if (command instanceof CEGWCellSizeAttributesRequest) {
            CEGWCellSizeAttributesRequest request = (CEGWCellSizeAttributesRequest) command;
            Map<RadioType, CellSizeAttributes> cellSizeAttributeMap = request.getCellSizeAttributesMap();

            String resultDetails = tipwlanOvsdbClient.processCellSizeAttributesRequest(inventoryId, cellSizeAttributeMap);
            response.setResultDetail(resultDetails);

        } else if (command instanceof CEGWFirmwareDownloadRequest) {

            CEGWFirmwareDownloadRequest dlRequest = (CEGWFirmwareDownloadRequest) command;

            String filepath = dlRequest.getFilePath();
            String firmwareVersion = dlRequest.getFirmwareVersion();
            String username = dlRequest.getUsername();

            String resultDetails = tipwlanOvsdbClient.processFirmwareDownload(inventoryId, filepath, firmwareVersion, username);

            response.setResultDetail(resultDetails);

        } else if (command instanceof CEGWFirmwareFlashRequest) {

            CEGWFirmwareFlashRequest flashRequest = (CEGWFirmwareFlashRequest) command;

            flashRequest.getFirmwareVersion();
            flashRequest.getInventoryId();
            flashRequest.getUsername();

            String resultDetails =
                    tipwlanOvsdbClient.processFirmwareFlash(flashRequest.getInventoryId(), flashRequest.getFirmwareVersion(), flashRequest.getUsername());

            response.setResultDetail(resultDetails);

        } else if (command instanceof CEGWRadioResetRequest) {
            response = new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand,
                    "Received Command " + command.getCommandType() + " for " + inventoryId, command,
                    registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());
        } else if (command instanceof CEGWRebootRequest) {

            CEGWRebootRequest rebootRequest = (CEGWRebootRequest) command;
            // Reboot the AP, Reset method specifies what kind of reboot, i.e.
            // Factory reset, reboot without changes, etc.
            EquipmentResetMethod resetMethod = rebootRequest.getPerformReset();
            switch (resetMethod) {
                case FactoryReset:
                    response.setResultDetail(tipwlanOvsdbClient.processFactoryResetRequest(inventoryId));
                    break;
                case NoReset:
                    if (rebootRequest.isUseInactiveBank()) {
                        response.setResultDetail(tipwlanOvsdbClient.processRebootRequest(inventoryId, true));
                    } else {
                        response.setResultDetail(tipwlanOvsdbClient.processRebootRequest(inventoryId, false));
                    }
                    break;
                case ConfigReset:
                case UNSUPPORTED: // for UNSUPPORTED or default just respond
                                  // with Unsupported Command
                default:
                    response = new EquipmentCommandResponse(CEGWCommandResultCode.UnsupportedCommand,
                            "Received Command " + command.getCommandType() + " for " + inventoryId, command,
                            registeredGateway == null ? null : registeredGateway.getHostname(), registeredGateway == null ? -1 : registeredGateway.getPort());
            }

        }

        return response;
    }

    private EquipmentCommandResponse processChangeRedirector(OvsdbSession session, CEGWChangeRedirectorHost command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processStartDebugEngine(OvsdbSession session, CEGWStartDebugEngine command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processStopDebugEngine(OvsdbSession session, CEGWStopDebugEngine command) {
        return sendMessage(session, command.getInventoryId(), command);
    }

    private EquipmentCommandResponse processBlinkRequest(OvsdbSession session, CEGWBlinkRequest command) {

        return sendMessage(session, command.getInventoryId(), command);
    }

    @RequestMapping(value = "/commandWithUser", method = RequestMethod.POST)
    public EquipmentCommandResponse sendCommandWithAuthUser(@RequestBody EquipmentCommand command, @AuthenticationPrincipal Object requestUser,
            HttpServletRequest httpServletRequest) {

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
                throw new ConfigurationException("Unable to register gateway with routing service: routing service interface not initialized");
            }

            cleanupStaleGwRecord();
            EquipmentGatewayRecord gwRecord = new EquipmentGatewayRecord();

            // external facing service, protected by the client certificate auth
            gwRecord.setHostname(getGatewayName());
            gwRecord.setIpAddr(connectorProperties.getExternalIpAddress().getHostAddress());
            gwRecord.setPort(connectorProperties.getExternalPort());

            try {

                EquipmentGatewayRecord result = eqRoutingSvc.registerGateway(gwRecord);
                registeredGwId = result.getId();
                registeredGateway = result;
                LOG.info("Successfully registered (name={}, id={}) with Routing Service", result.getHostname(), registeredGwId);
                registeredWithRoutingService = true;
            } catch (RuntimeException e) {
                // failed
                LOG.error("Failed to register Customer Equipment Gateway (name={}) with Routing Service : {}", getGatewayName(), e.getLocalizedMessage());
            }
        }
    }

    /**
     * This method does the following: See WIFI-540 1. Retrieves the existing
     * list of Gateway entries from the Routing Service 2. Check each one of
     * them for reachability (using PING method) 3. If the Gw does not respond
     * (stale IP), they will be unregistered/cleaned
     */
    protected void cleanupStaleGwRecord() {
        LOG.debug("In CleanUp stale registered Gateways records ");
        try {
            // Get Equipment gateway list
            List<EquipmentGatewayRecord> eqGwRecList = eqRoutingSvc.getGateway(GatewayType.CEGW);
            if (eqGwRecList != null) {
                for (EquipmentGatewayRecord eqpRec : eqGwRecList) {
                    if (!isGwReachable(eqpRec.getIpAddr(), eqpRec.getPort())) {
                        // GW isn't reachable --> invoke deleteGw
                        LOG.debug("Gateway {} is not-reachable... deleting from Routing Svc", eqpRec.getHostname());
                        try {
                            eqRoutingSvc.deleteGateway(eqpRec.getId());
                        } catch (RuntimeException e) {
                            // failed
                            LOG.error("Failed to delete Equipment Gateway (name={}) from Routing Service: {}", eqpRec.getHostname(), e.getLocalizedMessage());
                        }
                    } else {
                        LOG.debug("Gateway {} is reachable.", eqpRec.getHostname());
                    }
                }
            } else {
                LOG.debug("No gateways registered with Routing Service");
            }
        } catch (Exception ex) { // Catching Exception to prevent crashing the
                                 // register thread
            LOG.debug("Generic Exception encountered when trying to cleanup " + "the stale not-reachable GateWays. Continuing to register the new Gateway."
                    + " Error: {} ", ex.getMessage());
        }
    }

    private boolean isGwReachable(String ipAddr, int port) {
        return pingClient.isReachable(ipAddr, port);
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
                LOG.info("Deregistered Customer Equipment Gateway (name={},id={}) with Routing Service", getGatewayName(), registeredGwId);
                registeredGwId = -1;
                registeredGateway = null;
            } catch (Exception e) {
                // failed
                LOG.error("Failed to deregister Customer Equipment Gateway (name={},id={}) with Routing Service: {}", getGatewayName(), registeredGwId,
                        e.getLocalizedMessage());
            }
            registeredWithRoutingService = false;
        }
    }

    public long getRegisteredGwId() {
        return registeredGwId;
    }

    /**
     * Register a customer equipment with this gateway
     *
     * @param equipmentName
     * @param customerId
     * @param equipmentId
     * @return associationId
     */
    public EquipmentRoutingRecord registerCustomerEquipment(String equipmentName, Integer customerId, Long equipmentId) {
        registerWithRoutingService();
        if (!registeredWithRoutingService) {
            LOG.error("Unable to register customer equipement (name={},id={}): gateway not registered.", equipmentName, equipmentId);
            return null;
        }
        // Clean up stale records
        cleanupStaleEqptRoutingRecord(equipmentId);
        EquipmentRoutingRecord routingRecord = new EquipmentRoutingRecord();
        routingRecord.setCustomerId(customerId);
        routingRecord.setEquipmentId(equipmentId);
        routingRecord.setGatewayId(registeredGwId);
        try {
            routingRecord = eqRoutingSvc.create(routingRecord);

            LOG.debug("Registered customer equipment (name={},id={}) with route id={}", equipmentName, equipmentId, routingRecord.getId());
            return routingRecord;

        } catch (Exception e) {
            LOG.error("Failed to register customer equipement (name={},id={}): {}", equipmentName, equipmentId, e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Deletes the Equipment to Gateway relationship for gateway's that don't
     * respond See WIFI-540 1. Get List of EquipmentRoutingRecords for an
     * Equipment 2. Get the GW from GW-Id associated with 'this'
     * EquipmentRoutingRecord 3. Try to ping the gateway 4. If ping fails or
     * Gateway does not exist, delete the equipmentRouting entry.
     * 
     * @param equipmentId:
     *        Equipment's ID
     */
    protected void cleanupStaleEqptRoutingRecord(Long equipmentId) {
        LOG.debug("In Clean Up stale Equipment Routing record for Equipment ID {}", equipmentId);
        try {
            List<EquipmentRoutingRecord> eqptRoutingRecsList = eqRoutingSvc.getRegisteredRouteList(equipmentId);
            if (eqptRoutingRecsList != null) {
                for (EquipmentRoutingRecord eqRouting : eqptRoutingRecsList) {
                    try {
                        EquipmentGatewayRecord gwRec = eqRoutingSvc.getGateway(eqRouting.getGatewayId());
                        if (gwRec != null) {
                            if (!isGwReachable(gwRec.getIpAddr(), gwRec.getPort())) {
                                // GW isn't reachable --> invoke unregister
                                LOG.debug("Gateway {} is not-reachable... Deleting the equipment routing entry", gwRec.getHostname());
                                deleteUnresponiveGwRoutingRecord(eqRouting.getId(), equipmentId);
                            } else {
                                LOG.debug("Gateway {} is reachable.", gwRec.getHostname());
                            }
                        } else {
                            LOG.debug("Gateway with ID {} not found. Deleting the equipment routing entry ", eqRouting.getGatewayId());
                            deleteUnresponiveGwRoutingRecord(eqRouting.getId(), equipmentId);
                        }
                    } catch (DsEntityNotFoundException entityNotFoundException) {
                        LOG.debug("Gateway ID: {} not found... Deleting the equipment routing entry", eqRouting.getGatewayId());
                        deleteUnresponiveGwRoutingRecord(eqRouting.getId(), equipmentId);
                    }
                }
            } else {
                LOG.debug("No gateways registered with Routing Service for Equipment ID {}", equipmentId);
            }
        } catch (Exception genericException) { // Catching Exception to prevent
                                               // crashing the register thread
            LOG.debug(
                    "Generic Exception encountered when trying to cleanup "
                            + "the stale routing records for equipment ID: {}. Continuing to register the new RoutingRecord." + " Error: {} ",
                    equipmentId, genericException.getMessage());
        }
    }

    private boolean deleteUnresponiveGwRoutingRecord(Long routingId, Long eqptId) {
        try {
            eqRoutingSvc.delete(routingId);
        } catch (RuntimeException e) {
            // failed
            LOG.error("Failed to delete Equipment routing record (ID={}) from Routing Service: {}", eqptId, e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public void deregisterCustomerEquipment(Long routingId, String equipmentName, Long equipmentId) {
        if (!registeredWithRoutingService) {
            LOG.error("Unable to deregister customer equipement (name={},id={}): gateway not registered", equipmentName, equipmentId);
            return;
        }
        try {
            LOG.debug("Deregistering customer equipment (name={},id={}) with route id={}", equipmentName, equipmentId, routingId);

            eqRoutingSvc.delete(routingId);
        } catch (Exception e) {
            LOG.error("Failed to deregister customer equipement (name={},id={}) with route id={}: {}", equipmentName, equipmentId, routingId,
                    e.getLocalizedMessage());
        }
    }

    /**
     * Run every 5 minutes
     */
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedRate = 5 * 60 * 1000)
    public void updateActiveCustomer() {
        try {
            Map<Integer, Long> activeMap = getActiveCustomerMapForUpdate();
            if (null != activeMap) {
                LOG.info("Updating active customer records, total record size {}", activeMap.size());
                // this.eqRoutingSvc.updateActiveCustomer(activeMap,
                // getDeploymentId());
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
        activeCustomerReadLock.lock();
        try {
            activeCustomerMap.merge(customerId, System.currentTimeMillis(), latestTimestamp);
        } finally {
            activeCustomerReadLock.unlock();
        }
    }

    /**
     * Swap the active customer map for reporting if it contains records.
     *
     * @return null if no records.
     */
    protected Map<Integer, Long> getActiveCustomerMapForUpdate() {
        activeCustomerWriteLock.lock();
        try {
            Map<Integer, Long> map = null;
            if (!activeCustomerMap.isEmpty()) {
                map = activeCustomerMap;
                activeCustomerMap = new ConcurrentHashMap<>();
            }

            return map;
        } finally {
            activeCustomerWriteLock.unlock();
        }
    }
}
