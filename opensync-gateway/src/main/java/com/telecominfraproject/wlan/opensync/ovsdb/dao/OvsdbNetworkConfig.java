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
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.GreTunnelConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
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
    OvsdbGet ovsdbGet;

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
                    throw new RuntimeException("configureGreTunnel " + ((ErrorResult) res).getError() + " "
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

    /**
     *  @param ovsdbClient
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
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + " "
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
            WifiInetConfigInfo parentLanInterface = inetConfigMap.get(defaultLanInterfaceName);
            if (parentLanInterface == null) {
                throw new RuntimeException(
                        "Cannot get lan interface " + defaultLanInterfaceName + " for vlan " + vlanId);
            }
            tableColumns.put("if_type", new Atom<>("vlan"));
            tableColumns.put("vlan_id", new Atom<>(vlanId));
            tableColumns.put("if_name", new Atom<>(parentLanInterface.ifName + "_" + Integer.toString(vlanId)));
            tableColumns.put("parent_ifname", new Atom<>(parentLanInterface.ifName));
            tableColumns.put("enabled", new Atom<>(true));
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("ip_assign_scheme", new Atom<>(parentLanInterface.ipAssignScheme));
            tableColumns.put("NAT", new Atom<>(parentLanInterface.nat));
            tableColumns.put("mtu", new Atom<>(1500));
            String[] inetAddress = parentLanInterface.inetAddr.split("\\.");
            String vlanAddress = inetAddress[0] + "." + inetAddress[1] + "." + vlanId + "." + inetAddress[3];
            tableColumns.put("inet_addr", new Atom<>(vlanAddress));
            tableColumns.put("netmask", new Atom<>(parentLanInterface.netmask));
            tableColumns.put("dhcpd", com.vmware.ovsdb.protocol.operation.notation.Map.of(parentLanInterface.dhcpd));
            Row row = new Row(tableColumns);
            if (inetConfigMap.containsKey(parentLanInterface.ifName + "_" + Integer.toString(vlanId))) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(new Condition("vlan_id", Function.EQUALS, new Atom<>(vlanId)));
                conditions.add(new Condition("parent_ifname", Function.EQUALS, new Atom<>(parentLanInterface.ifName)));
                operations.add(new Update(wifiInetConfigDbTable, conditions, row));
            } else {
                operations.add(new Insert(wifiInetConfigDbTable, row));
            }
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
            tableColumns.put("network", new Atom<>(true));
            tableColumns.put("ip_assign_scheme", new Atom<>(parentWanInterface.ipAssignScheme));
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
                    throw new RuntimeException("createVlanNetworkInterfaces " + ((ErrorResult) res).getError() + " "
                            + ((ErrorResult) res).getDetails());
                }
            }
            inetConfigMap = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient);
            LOG.debug("Provisioned vlan on wan {} and lan {}",
                    inetConfigMap.get(parentWanInterface.ifName + "_" + Integer.toString(vlanId)),
                    inetConfigMap.get(parentLanInterface.ifName + "_" + Integer.toString(vlanId)));
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
            List<Condition> conditions = new ArrayList<>();
            for (WifiInetConfigInfo wifiInetConfigInfo : provisionedWifiInetConfigs) {
                if (wifiInetConfigInfo.vlanId > 1 || wifiInetConfigInfo.ifType.equals("vif")
                        || wifiInetConfigInfo.ifName.startsWith("gre") || wifiInetConfigInfo.ifType.equals("gre")) {
                    conditions = new ArrayList<>();
                    conditions.add(new Condition("if_name", Function.EQUALS, new Atom<>(wifiInetConfigInfo.ifName)));
                    operations.add(new Delete(wifiInetConfigDbTable, conditions));
                }
            }
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            LOG.info("Removed all existing vif, vlan, and gre interface configs from {}:", wifiInetConfigDbTable);
            for (OperationResult res : result) {
                LOG.info("Op Result {}", res);
            }
            provisionedWifiInetConfigs = ovsdbGet.getProvisionedWifiInetConfigs(ovsdbClient).values();
            for (WifiInetConfigInfo inetConfigInfo : provisionedWifiInetConfigs) {
                if (inetConfigInfo.ifType.equals("vif") || inetConfigInfo.ifType.equals("gre")) {
                    throw new RuntimeException(
                            "Failed to remove all vif and gre interface configurations from Wifi_Inet_Config dbTable, still has "
                                    + provisionedWifiInetConfigs.stream().filter(new Predicate<WifiInetConfigInfo>() {
                                        @Override
                                        public boolean test(WifiInetConfigInfo t) {
                                            if ((t.ifType.equals("vif")) || (t.ifType.equals("gre"))) {
                                                return true;
                                            }
                                            return false;
                                        }
                                    }).collect(Collectors.toList()));

                }
            }
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeAllInetConfigs", e);
            throw new RuntimeException(e);
        }
    }
}
