package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.equipment.models.NetworkForwardMode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.WifiInetConfigInfo;
import com.telecominfraproject.wlan.profile.ethernetport.models.WiredEthernetPortConfiguration;
import com.telecominfraproject.wlan.profile.ethernetport.models.WiredPort;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.GreTunnelConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.status.StatusServiceInterface;
import com.telecominfraproject.wlan.status.equipment.report.models.WiredEthernetPortStatusData;
import com.telecominfraproject.wlan.status.equipment.report.models.WiredPortStatus;
import com.telecominfraproject.wlan.status.models.Status;
import com.telecominfraproject.wlan.status.models.StatusDataType;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbNetworkConfig extends OvsdbDaoBase {

    @Autowired
    private OvsdbGet ovsdbGet;
    @Autowired
    private StatusServiceInterface statusServiceInterface;

    private static final String VLAN_IF_TYPE = "vlan";
    private static final String VLAN_TRUNK_IF_TYPE = "vlan_trunk";
    private static final String BRIDGE_IF_TYPE = "bridge";
    private static final String LAN_IF_NAME = "lan";
    private static final String WAN_IF_NAME = "wan";
    private static final int DEFAULT_MAX_INTERNAL_VLAN_ID = 2;
    private static final String SEPARATOR = " ";


    void configureGreTunnel(OvsdbClient ovsdbClient, Profile apNetworkConfiguration) {
        try {
            LOG.debug("Configure Gre Tunnel {}", apNetworkConfiguration);
            List<Operation> operations = new ArrayList<>();

            ApNetworkConfiguration details = (ApNetworkConfiguration) apNetworkConfiguration.getDetails();

            for (GreTunnelConfiguration greTunnelConfiguration : details.getGreTunnelConfigurations()) {

                if (greTunnelConfiguration.getGreRemoteInetAddr() == null) {
                    LOG.info("Cannot configure GRE profile without gre_remote_inet_addr");
                    continue;
                }

                if (greTunnelConfiguration.getGreTunnelName() == null) {
                    LOG.info("Cannot configure GRE profile without if_name");
                    continue;
                }

                Map<String, Value> tableColumns = new HashMap<>();
                tableColumns.put("gre_remote_inet_addr",
                        new Atom<>(greTunnelConfiguration.getGreRemoteInetAddr().getHostAddress()));
                tableColumns.put("if_name", new Atom<>(greTunnelConfiguration.getGreTunnelName()));
                tableColumns.put("if_type", new Atom<>("gre"));
                tableColumns.put("enabled", new Atom<>(true));
                tableColumns.put("network", new Atom<>(true));

                operations.add(new Insert(wifiInetConfigDbTable, new Row(tableColumns)));

            }

            if (operations.isEmpty()) {
                LOG.info("No GRE tunnels to be configured.");
                return;
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("configureGreTunnel {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {
                    LOG.info("configureGreTunnel {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("configureGreTunnel error {}", (res));
                    throw new RuntimeException("configureGreTunnel " + ((ErrorResult) res).getError() + SEPARATOR
                            + ((ErrorResult) res).getDetails());
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Couldn't configure Gre Tunnel {}", apNetworkConfiguration, e);
            throw new RuntimeException(e);
        }
    }

    void configureGreTunnels(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {

        LOG.info("Configure Gre tunnels {}", opensyncApConfig.getApProfile());
        if (opensyncApConfig.getApProfile() != null) {
            configureGreTunnel(ovsdbClient, opensyncApConfig.getApProfile());
        }

    }

    /**
     * Insert or update Wifi_Inet_Interface for Wifi_VIF_Config table entry
     *
     * @param ovsdbClient
     * @param ifName
     * @param enabled
     * @param ifType
     * @param isUpdate
     * @param isNat
     */
    void configureInetInterface(OvsdbClient ovsdbClient, String ifName, boolean enabled, String ifType,
            boolean isUpdate, boolean isNat, List<Operation> operations) {
            Map<String, Value> tableColumns = new HashMap<>();
            tableColumns.put("if_type", new Atom<>(ifType));
            tableColumns.put("enabled", new Atom<>(enabled));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("if_name", new Atom<>(ifName));
            tableColumns.put("NAT", new Atom<>(isNat));
            Row row = new Row(tableColumns);
            if (isUpdate) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(ifName)));
                operations.add(new Update(wifiInetConfigDbTable, conditions, row));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }
            
    }

	void configureEthernetPorts(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
		Profile wiredEthernetPortProfile = opensyncApConfig.getWiredEthernetPortProfile();

		if (wiredEthernetPortProfile != null) {
            LOG.debug("Entering configure Ethernet Ports for wiredEthernetPortProfile {} for equipment {}",
                    wiredEthernetPortProfile, opensyncApConfig.getCustomerEquipment().getId());
			// Getting the config from the UI
			WiredEthernetPortConfiguration ethernetPortConfig = (WiredEthernetPortConfiguration) wiredEthernetPortProfile
					.getDetails();
			LOG.debug("Configure EthernetPorts {}", ethernetPortConfig);
			Set<WiredPort> desiredWiredPorts = ethernetPortConfig.getEthPorts();

			handleEthernetPortConfiguration(ovsdbClient, desiredWiredPorts);

		} else {
		    LOG.info("opensyncApConfig for Customer {}, Equipment {} has no wiredEthernetPortProfile attached" ,
                    opensyncApConfig.getCustomerEquipment().getCustomerId(), opensyncApConfig.getCustomerEquipment().getId());
        }
	}

	private void handleEthernetPortConfiguration(OvsdbClient ovsdbClient, Set<WiredPort> desiredWiredPorts) {
        // lists are for more than one lan port moving to wan ("eth0 eth1 eth2")
        Set<String> availableLanEthNames = new HashSet<>();
        Set<String> availableWanEthNames = new HashSet<>();
        if (desiredWiredPorts == null) {
            LOG.info("Cannot handle the EthernetPort config for null desired Wired Ports. Exiting");
            return;
        }
        LOG.debug("In handleEthernetPortConfiguration with desiredWiredPorts {}", desiredWiredPorts);
        WiredPort wanPort = getDefaultWanPort(desiredWiredPorts);
        if (wanPort == null) {
            LOG.error("Cannot find the default wan port from the desired list of ports {}" , desiredWiredPorts);
            return;
        }
        for (WiredPort port: desiredWiredPorts) {
        	if (port.getIfName().contains(LAN_IF_NAME)) { // for AP has ifName lan1, lan2 instead of lan
        		availableLanEthNames.add(port.getName());
        	} else if (WAN_IF_NAME.equals(port.getIfName())) {
        		availableWanEthNames.add(port.getName());
        	}
        }
        for (WiredPort desiredPort : desiredWiredPorts) {
            if (!desiredPort.equals(wanPort)) {
                if (isLanToBridgeConversion(desiredPort) && !desiredPort.isTrunkEnabled()) {
                    // Flow 1: Desired Bridge + Access
                    LOG.debug("Desired Config is Bridge with Access Mode");
                    availableLanEthNames.remove(desiredPort.getName());
                    availableWanEthNames.add(desiredPort.getName());
                    changeLanToWan(ovsdbClient, availableLanEthNames, availableWanEthNames);
                    createVlanTag(ovsdbClient, desiredPort);
                } else if (isLanToBridgeConversion(desiredPort) && desiredPort.isTrunkEnabled()) {
                    // Flow 2: Desired Bridge + Trunk
                    LOG.debug("Desired Config is Bridge with Trunk Mode");
                    availableLanEthNames.remove(desiredPort.getName());
                    availableWanEthNames.add(desiredPort.getName());
                    changeLanToWan(ovsdbClient, availableLanEthNames, availableWanEthNames);
                    createTrunkInterface(ovsdbClient, desiredPort);
                } else if (!BRIDGE_IF_TYPE.equalsIgnoreCase(desiredPort.getIfType())) {
                    // Flow 3: Desired ifType is NAT
                    LOG.debug("Desired Config is NAT (LAN) mode. Desired Port {} may still be tagged to WAN interface. Convert to LAN mode", desiredPort.getIfName());
                    availableLanEthNames.add(desiredPort.getName());
                    availableWanEthNames.remove(desiredPort.getName());
                    changeWanToLan(ovsdbClient, availableLanEthNames, availableWanEthNames);
                } else {
                    LOG.info("Not a supported Config change requested for the port {}", desiredPort);
                }
            } else {
                LOG.error("It's the default WAN port. No changes needed!");
            }
        } // end of for loop
    }

    private String generateInterfaceId(String portName, int vlanId, boolean trunk) {
        if (!trunk) {
            return String.format("%s_%s", portName, vlanId);
        } else {
            return String.format("%s_trunk", portName);
        }
    }

    private boolean isLanToBridgeConversion(WiredPort desiredPort) {
        return LAN_IF_NAME.equalsIgnoreCase(desiredPort.getIfName()) &&
                BRIDGE_IF_TYPE.equals(desiredPort.getIfType());
    }

	WiredPort getDefaultWanPort(Set<WiredPort> desiredPorts) {
		WiredPort wanWiredPort = desiredPorts.stream()
				.filter(entry -> WAN_IF_NAME.equals(entry.getIfName())).findFirst().orElse(null);

		LOG.debug("Returning wanWiredPort {}", wanWiredPort);
		return wanWiredPort;
	}

	void changeLanToWan(OvsdbClient ovsdbClient, Set<String> lanPortsToUpdate, Set<String> wanPortsToUpdate) {
		LOG.debug("Calling changeLanToWan: moving port {} to LAN and {} to WAN", lanPortsToUpdate, wanPortsToUpdate);
		List<Operation> operations = new ArrayList<>();
		// Step1: remove lan from eth_ports
		// /usr/opensync/bin/ovsh u Wifi_Inet_Config eth_ports:=" " -w if_name==lan
		// or cases like: /usr/opensync/bin/ovsh u Wifi_Inet_Config eth_ports:="eth2" -w if_name==lan
		String lanPortsString = String.join(SEPARATOR,  lanPortsToUpdate);
		addEthPortsOperation(operations, lanPortsString.equals("")? SEPARATOR : lanPortsString, LAN_IF_NAME);
		
		// Step2: make lan to a wan port
		// /usr/opensync/bin/ovsh u Wifi_Inet_Config eth_ports:="eth0 eth1" -w if_name==wan
		// eth_ports syntax is set by AP (i.e, eth0 eth1)
		addEthPortsOperation(operations, String.join(SEPARATOR,  wanPortsToUpdate), WAN_IF_NAME);
		sendOperationsToAP(ovsdbClient, operations, "changeLanToWan");
	}

	private void createVlanTag(OvsdbClient ovsdbClient, WiredPort desiredWiredPort) {
        LOG.debug("Creating a new VlanInterface for wiredPort {}", desiredWiredPort);
        if (desiredWiredPort.getVlanId() > DEFAULT_MAX_INTERNAL_VLAN_ID) {
            // Tag to vlan
            // /usr/opensync/bin/ovsh i Wifi_Inet_Config NAT:=true enabled:=true
            // if_name:=eth0_100 if_type:=vlan ip_assign_scheme:=none network:=true
            // parent_ifname:=eth0 vlan_id:=100 dhcp_sniff:=false eth_ports:="eth0"
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();
            tableColumns.put("NAT", new Atom<>(true));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("if_name", new Atom<>(generateInterfaceId(desiredWiredPort.getName(), desiredWiredPort.getVlanId(), false)));
            tableColumns.put("if_type", new Atom<>(VLAN_IF_TYPE));
            tableColumns.put("ip_assign_scheme", new Atom<>("none"));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("parent_ifname", new Atom<>(desiredWiredPort.getName()));
            tableColumns.put("vlan_id", new Atom<>(desiredWiredPort.getVlanId()));
            tableColumns.put("dhcp_sniff", new Atom<>(false));
            tableColumns.put("eth_ports", new Atom<>(desiredWiredPort.getName()));
            Row row = new Row(tableColumns);
            operations.add(new Insert(wifiInetConfigDbTable, row));

            sendOperationsToAP(ovsdbClient, operations, "createVlanTag");
        } else {
            LOG.info("Desired vlanId {} is lower than default {}; no tagging needed",
                    desiredWiredPort.getVlanId(), DEFAULT_MAX_INTERNAL_VLAN_ID);
        }
    }

    private void createTrunkInterface(OvsdbClient ovsdbClient, WiredPort desiredWiredPort) {
        LOG.debug("Creating a new TrunkInterface for wiredPort {}", desiredWiredPort);

        // Create Trunk Interface
        // /usr/opensync/bin/ovsh i Wifi_Inet_Config NAT:=true enabled:=true
        // if_name:=eth0_trunk if_type:=vlan_trunk ip_assign_scheme:=none network:=true
        // parent_ifname:=eth0 dhcp_sniff:=false eth_ports:="eth0"
        // vlan_trunk:='["map",[["allowed_vlans","100 200 300"],["pvid","10"]]]'

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> tableColumns = new HashMap<>();
        tableColumns.put("NAT", new Atom<>(true));
        tableColumns.put("enabled", new Atom<>(true));
        tableColumns.put("if_name", new Atom<>(generateInterfaceId(desiredWiredPort.getName(), desiredWiredPort.getVlanId(), true)));
        tableColumns.put("if_type", new Atom<>(VLAN_TRUNK_IF_TYPE));
        tableColumns.put("ip_assign_scheme", new Atom<>("none"));
        tableColumns.put("network", new Atom<>(true));
        tableColumns.put("parent_ifname", new Atom<>(desiredWiredPort.getName()));
        tableColumns.put("dhcp_sniff", new Atom<>(false));
        tableColumns.put("eth_ports", new Atom<>(desiredWiredPort.getName()));

        Map<String, String> vlanTrunkMap = new HashMap<>();

        if (desiredWiredPort.getAllowedVlanIds() != null) {
            vlanTrunkMap.put("allowed_vlans", getAllowedVlanAsString(desiredWiredPort));
            if (desiredWiredPort.getVlanId() > DEFAULT_MAX_INTERNAL_VLAN_ID) {
                vlanTrunkMap.put("pvid", String.valueOf(desiredWiredPort.getVlanId()));
            }
        }

        com.vmware.ovsdb.protocol.operation.notation.Map<String, String> vlanTrunkVal = com.vmware.ovsdb.protocol.operation.notation.Map.of(vlanTrunkMap);

        tableColumns.put(VLAN_TRUNK_IF_TYPE, vlanTrunkVal);
        Row row = new Row(tableColumns);
        operations.add(new Insert(wifiInetConfigDbTable, row));

        sendOperationsToAP(ovsdbClient, operations, "createTrunkInterface");

    }

    private String getAllowedVlanAsString(WiredPort desiredWiredPort) {
        return desiredWiredPort.getAllowedVlanIds().stream().map(String::valueOf).collect(Collectors.joining(SEPARATOR));
    }

	void changeWanToLan(OvsdbClient ovsdbClient, Set<String> lanPortsToUpdate, Set<String> wanPortsToUpdate) {
		LOG.debug("Calling changeWanToLan: Moving port {} to LAN and {} to WAN", lanPortsToUpdate, wanPortsToUpdate);
		// Step1: set the correct port to lan
		// /usr/opensync/bin/ovsh u Wifi_Inet_Config eth_ports:="eth0" -w if_name==lan
		List<Operation> operations = new ArrayList<>();
		addEthPortsOperation(operations, String.join(SEPARATOR,  lanPortsToUpdate), LAN_IF_NAME);

		// Step2: set the correct port to wan
		// /usr/opensync/bin/ovsh u Wifi_Inet_Config eth_ports:="eth1" -w if_name==wan
		addEthPortsOperation(operations, String.join(SEPARATOR,  wanPortsToUpdate), WAN_IF_NAME);

		sendOperationsToAP(ovsdbClient, operations, "changeWanToLan");
	}
	
	void addEthPortsOperation(List<Operation> operations, String ethPorts, String ifName) {
		Map<String, Value> tableColumn = new HashMap<>();
		tableColumn.put("eth_ports", new Atom<>(ethPorts));
		Row row = new Row(tableColumn);
		operations.add(new Update(wifiInetConfigDbTable,
				List.of(new Condition("if_name", Function.EQUALS, new Atom<>(ifName))), row));
	}

	void sendOperationsToAP(OvsdbClient ovsdbClient, List<Operation> operations, String methodName) {
		try {
			CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
			OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
			for (OperationResult res : result) {
				if (res instanceof InsertResult) {
					LOG.info("{} - {}", methodName, res);
				} else if (res instanceof UpdateResult) {
					LOG.info("{} {}", methodName, res);
				} else if (res instanceof ErrorResult) {
					LOG.error("{} {}", methodName, res);
					throw new RuntimeException("createVlanNetworkInterfaces - " + ((ErrorResult) res).getError() + SEPARATOR
							+ ((ErrorResult) res).getDetails());
				}
			}
		} catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * @param ovsdbClient
     * @param vifInterfaceName
     * @param enabled
     * @param networkForwardMode
     * @param operations
     */
    void configureInetVifInterface(OvsdbClient ovsdbClient, String vifInterfaceName, boolean enabled,
                                   NetworkForwardMode networkForwardMode, List<Operation> operations) {
        Map<String, WifiInetConfigInfo> inetConfigs = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
        if (inetConfigs.containsKey(vifInterfaceName)) {
            configureInetInterface(ovsdbClient, vifInterfaceName, enabled, "vif", true,
                    (networkForwardMode == NetworkForwardMode.NAT), operations);
        } else {
            configureInetInterface(ovsdbClient, vifInterfaceName, enabled, "vif", false,
                    (networkForwardMode == NetworkForwardMode.NAT), operations);
        }
    }

    /*
     * Use this to do any post configuration interface adjustment (i.e. turn on
     * dhcp_sniff, etc.)
     */
    void configureInterfaces(OvsdbClient ovsdbClient) {
        configureWanInterfacesForDhcpSniffing(ovsdbClient);
    }

    void createVlanInterfaceInGreTunnel(OvsdbClient ovsdbClient, int vlanId, String greTunnel) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();
            Map<String, WifiInetConfigInfo> inetConfigMap = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
            WifiInetConfigInfo parentTunnel = inetConfigMap.get(greTunnel);
            if (parentTunnel == null) {
                throw new RuntimeException("Cannot get tunnel interface " + parentTunnel + " for vlan " + vlanId);
            }
            tableColumns = new HashMap<>();
            tableColumns.put("if_type", new Atom<>("bridge"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>(parentTunnel.ifName + "_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>(parentTunnel.ifName));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("network", new Atom<>(true));
            Row row = new Row(tableColumns);
            operations.add(new Insert(wifiInetConfigDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("createVlanNetworkInterfaces error {}", (res));
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + SEPARATOR
                            + ((ErrorResult) res).getDetails());
                }
            }
            inetConfigMap = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
            LOG.debug("Provisioned vlan on greTunnel {}",
                    inetConfigMap.get(parentTunnel.ifName + "_" + Integer.toString(vlanId)));
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in provisioning Vlan", e);
            throw new RuntimeException(e);
        }

    }

    void createVlanNetworkInterfaces(OvsdbClient ovsdbClient, int vlanId) {
        try {
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> tableColumns = new HashMap<>();
            Map<String, WifiInetConfigInfo> inetConfigMap = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
            Row row = new Row(tableColumns);

            WifiInetConfigInfo parentWanInterface = inetConfigMap.get(defaultWanInterfaceName);
            if (parentWanInterface == null) {
                throw new RuntimeException(
                        "Cannot get wan interface " + defaultWanInterfaceName + " for vlan " + vlanId);
            }
            tableColumns = new HashMap<>();
            tableColumns.put("if_type", new Atom<>("vlan"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>(parentWanInterface.ifName + "_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>(parentWanInterface.ifName));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("ip_assign_scheme", new Atom<>("none"));
            tableColumns.put("NAT", new Atom<>(parentWanInterface.nat));
            tableColumns.put("mtu", new Atom<>(1500));
            row = new Row(tableColumns);
            if (inetConfigMap.containsKey(parentWanInterface.ifName + "_" + Integer.toString(vlanId))) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("vlan_id", Function.EQUALS, new Atom<>(vlanId)));
                conditions.add(new Condition("parent_ifname", Function.EQUALS, new Atom<>(parentWanInterface.ifName)));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult res : result) {
                if (res instanceof InsertResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((InsertResult) res).toString());
                } else if (res instanceof UpdateResult) {
                    LOG.info("createVlanNetworkInterfaces {}", ((UpdateResult) res).toString());
                } else if (res instanceof ErrorResult) {
                    LOG.error("createVlanNetworkInterfaces error {}", (res));
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + SEPARATOR
                            + ((ErrorResult) res).getDetails());
                }
            }
            inetConfigMap = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
            LOG.debug("Provisioned vlan on wan {} and lan {}",
                    inetConfigMap.get(parentWanInterface.ifName + "_" + Integer.toString(vlanId)));
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in provisioning Vlan", e);
            throw new RuntimeException(e);
        }
    }

    void createVlanNetworkInterfaces(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
        Set<Integer> vlans = new HashSet<>();
        for (Profile ssidProfile : opensyncApConfig.getSsidProfile()) {
            Integer vlanId = ((SsidConfiguration) ssidProfile.getDetails()).getVlanId();
            if (vlanId != null && vlanId > 1) {
                vlans.add(vlanId);
            }
        }
        for (Integer vlanId : vlans) {
            Optional<GreTunnelConfiguration> tunnelConfiguration = ((ApNetworkConfiguration) opensyncApConfig
                    .getApProfile().getDetails()).getGreTunnelConfigurations().stream()
                            .filter(new Predicate<GreTunnelConfiguration>() {
                                @Override
                                public boolean test(GreTunnelConfiguration t) {
                                    return t.getVlanIdsInGreTunnel().contains(vlanId);
                                }
                            }).findFirst();
            if (tunnelConfiguration.isPresent()) {
                createVlanInterfaceInGreTunnel(ovsdbClient, vlanId, tunnelConfiguration.get().getGreTunnelName());
            } else {
                createVlanNetworkInterfaces(ovsdbClient, vlanId);
            }
        }
    }

    void removeAllInetConfigs(OvsdbClient ovsdbClient) {
        try {
            Collection<WifiInetConfigInfo> provisionedWifiInetConfigs = ovsdbGet
                    .getProvisionedWifiInetConfigs(ovsdbClient).values();
            List<Operation> operations = new ArrayList<>();
            for (WifiInetConfigInfo wifiInetConfigInfo : provisionedWifiInetConfigs) {
                if (wifiInetConfigInfo.vlanId > 1
                        || wifiInetConfigInfo.ifType.equals("vif")
                        || wifiInetConfigInfo.ifName.startsWith("gre") || wifiInetConfigInfo.ifType.equals("gre")
                        || VLAN_IF_TYPE.equalsIgnoreCase(wifiInetConfigInfo.ifType) // Remove any existing vlan/vlan_trunk tagging
                        || VLAN_TRUNK_IF_TYPE.equalsIgnoreCase(wifiInetConfigInfo.ifType) ) {
                    List<Condition> conditions = new ArrayList<>();
                    conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(wifiInetConfigInfo.ifName)));
                    operations.add(new Delete(wifiInetConfigDbTable, conditions));
                }
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            LOG.info("Removed all existing vif, vlan, vlan_trunk and gre interface configs from {}:", wifiInetConfigDbTable);
            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
            }
            provisionedWifiInetConfigs = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient).values();
            for (WifiInetConfigInfo inetConfigInfo : provisionedWifiInetConfigs) {
                if (inetConfigInfo.ifType.equals("vif") || inetConfigInfo.ifType.equals("gre") ||
                        VLAN_IF_TYPE.equals(inetConfigInfo.ifType) || VLAN_TRUNK_IF_TYPE.equals(inetConfigInfo.ifType)) {
                    throw new RuntimeException(
                            "Failed to remove all vif, gre, vlan interface configurations from Wifi_Inet_Config dbTable, still has "
                                    + provisionedWifiInetConfigs.stream().filter(new Predicate<WifiInetConfigInfo>() {
                                        @Override
                                        public boolean test(WifiInetConfigInfo t) {
                                            return (t.ifType.equals("vif")) || (t.ifType.equals("gre")) ||
                                                  VLAN_IF_TYPE.equals(t.ifType) || VLAN_TRUNK_IF_TYPE.equals(t.ifType);
                                        }
                                    }).collect(Collectors.toList()));

                }
            }
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllInetConfigs", e);
            throw new RuntimeException(e);
        }
    }
    
	// Reset all LAN ports from WAN back to LAN (NAT mode)
	void resetWiredPorts(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncApConfig) {
		LOG.debug("Calling resetWiredPorts");
		// e.g. eth1
		Map<String, List<WiredPortStatus>> interfacePortStatusMap = getInterfacePortStatus(
				opensyncApConfig.getCustomerEquipment().getCustomerId(),
				opensyncApConfig.getCustomerEquipment().getId());
		if (interfacePortStatusMap == null) {
			LOG.info("No InterfacePort(EthernetPort) Status. Exiting");
			return;
		}
		List<WiredPortStatus> wanPortStatusList = interfacePortStatusMap.get(WAN_IF_NAME);
		if (wanPortStatusList == null) {
			LOG.info("No wan interface status available. Exiting");
			return;
		}
		WiredPortStatus originalWanPortStatus = wanPortStatusList.stream()
				.filter(wan -> WAN_IF_NAME.equals(wan.getOriginalIfName())).findFirst().orElse(null);
		if (originalWanPortStatus == null) {
			LOG.info("No original wan port available. Exiting");
			return;
		}
		String wanEthName = originalWanPortStatus.getName();
		Set<String> lanEthNames = new HashSet<>();
		for (WiredPortStatus wanPortStatus : wanPortStatusList) {
			if (wanPortStatus.getOriginalIfName().contains(LAN_IF_NAME)) {
				lanEthNames.add(wanPortStatus.getName());
			}
		}
		if (!lanEthNames.isEmpty()) {
			changeWanToLan(ovsdbClient, lanEthNames, Set.of(wanEthName));
		}
	}

	private Map<String, List<WiredPortStatus>> getInterfacePortStatus(int customerId, long equipmentId) {
		Map<String, List<WiredPortStatus>> interfacePortStatus = null;
		// Getting the status from the current AP
		Status existingPortStatus = statusServiceInterface.getOrNull(customerId, equipmentId,
				StatusDataType.WIRED_ETHERNET_PORT);
		if (existingPortStatus != null && existingPortStatus.getDetails() instanceof WiredEthernetPortStatusData) {
			interfacePortStatus = ((WiredEthernetPortStatusData) existingPortStatus.getDetails())
					.getInterfacePortStatusMap();
		}

		if (interfacePortStatus == null || interfacePortStatus.isEmpty()) {
			LOG.info("No ethernetPortStatus found for customer {}, equipment {}", customerId, equipmentId);
		}
		return interfacePortStatus;
	}
}
