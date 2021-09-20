
package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.telecominfraproject.wlan.core.model.equipment.LedStatus;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
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
import com.vmware.ovsdb.protocol.operation.result.InsertResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbNodeConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet ovsdbGet;

    public void configureNtpServer(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        try {
            ApNetworkConfiguration apNetworkConfig = (ApNetworkConfiguration) opensyncAPConfig.getApProfile().getDetails();
            if (apNetworkConfig.getNtpServer() == null || apNetworkConfig.getNtpServer().getValue() == null) {
                LOG.info("Cannot configure NTP server to null value. {}", apNetworkConfig);
                return;
            }
            LOG.debug("configureNtpServer update Node_Config to {}", apNetworkConfig.getNtpServer().getValue());
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("value", new Atom<>(apNetworkConfig.getNtpServer().getValue()));
            List<Operation> operations = new ArrayList<>();
            operations.add(new Update(nodeConfigTable, List.of(new Condition("module", Function.EQUALS, new Atom<>("ntp"))), new Row(updateColumns)));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult res : result) {
                LOG.debug("configureNtpServer result {}", res);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public void configureSyslog(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        // /usr/opensync/tools/ovsh insert Node_Config module:="syslog" key:="remote" value:="udp:192.168.178.9:1000:4"
        // The format is a colon delimited list. log_proto:log_ip:log_port:log_priority
        try {
            ApNetworkConfiguration apNetworkConfig = (ApNetworkConfiguration) opensyncAPConfig.getApProfile().getDetails();
            if (apNetworkConfig.getSyslogRelay() == null) {
                LOG.info("Cannot configure syslog to null value. {}", apNetworkConfig);
                return;
            }

            if (apNetworkConfig.getSyslogRelay().isEnabled()) {

                if (apNetworkConfig.getSyslogRelay().getSrvHostIp() == null || apNetworkConfig.getSyslogRelay().getSeverity() == null) {
                    LOG.info("Cannot configure syslog remote_logging without SrvHostIp and Severity values. {}", apNetworkConfig);
                    return;
                }
                LOG.debug("configureSyslog remote_logging to {}", apNetworkConfig.getSyslogRelay());
                Map<String, Value> columns = new HashMap<>();
                columns.put("key", new Atom<>("remote"));
                columns.put("module", new Atom<>("syslog"));
                String delimitedValue = "udp:" + apNetworkConfig.getSyslogRelay().getSrvHostIp().getHostAddress() + ":" + String.valueOf(
                        apNetworkConfig.getSyslogRelay().getSrvHostPort() + ":" + String.valueOf(apNetworkConfig.getSyslogRelay().getSeverity().getId()));
                columns.put("value", new Atom<>(delimitedValue));
                List<Operation> operations = new ArrayList<>();
                operations.add(new Update(nodeConfigTable, List.of(new Condition("module", Function.EQUALS, new Atom<>("syslog"))), new Row(columns)));
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                long numUpdates = 0;
                for (OperationResult res : result) {
                    if (res instanceof UpdateResult) {
                        numUpdates += ((UpdateResult) res).getCount();
                        LOG.debug("configureSyslog update result {}", res);
                    }
                }
                if (numUpdates == 0) {
                    // no records existed, insert the row instead
                    operations.clear();
                    operations.add(new Insert(nodeConfigTable, new Row(columns)));
                    fResult = ovsdbClient.transact(ovsdbName, operations);
                    result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                    for (OperationResult res : result) {
                        LOG.debug("configureSyslog insert result {}", res);
                    }
                }
            } else {
                LOG.debug("Disable remote_logging", apNetworkConfig.getSyslogRelay());
                List<Operation> operations = new ArrayList<>();
                operations.add(new Delete(nodeConfigTable, List.of(new Condition("module", Function.EQUALS, new Atom<>("syslog")))));
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                for (OperationResult res : result) {
                    if (res instanceof UpdateResult) {
                        LOG.debug("configureSyslog disabled remote_logging {}", res);
                    }
                }
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    public String processBlinkRequest(OvsdbClient ovsdbClient, String apId, boolean blinkAllLEDs) {

        String ret = null;
        try {

            LOG.debug("processLEDRequest set LEDs status to {}", blinkAllLEDs);
            Map<String, Value> columns = new HashMap<>();
            if (blinkAllLEDs) {
                columns.put("module", new Atom<>("led"));
                columns.put("key", new Atom<>("led_blink"));
            } else {
                columns.put("module", new Atom<>("led"));
                columns.put("key", new Atom<>("led_state"));
            }
            List<Operation> operations = new ArrayList<>();
            operations.add(new Update(nodeConfigTable, List.of(new Condition("module", Function.EQUALS, new Atom<>("led"))), new Row(columns)));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            long numUpdates = 0;
            for (OperationResult res : result) {
                if (res instanceof UpdateResult) {
                    numUpdates += ((UpdateResult) res).getCount();
                    LOG.debug("processBlinkRequest update result {}", res);
                    ret = "processBlinkRequest update result " + res;
                }
            }
            if (numUpdates == 0) {
                // no records existed, insert the row instead
                operations.clear();
                operations.add(new Insert(nodeConfigTable, new Row(columns)));
                fResult = ovsdbClient.transact(ovsdbName, operations);
                result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                for (OperationResult res : result) {
                    if (res instanceof InsertResult) {
                        LOG.debug("processBlinkRequest insert result {}", res);
                        ret = "processBlinkRequest insert result " + res;
                    }
                }
            }
            
            return ret;
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
