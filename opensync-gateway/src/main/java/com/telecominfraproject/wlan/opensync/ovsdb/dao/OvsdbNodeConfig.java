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

import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPConfig;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Condition;
import com.vmware.ovsdb.protocol.operation.notation.Function;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbNodeConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet ovsdbGet;

    public void configureNtpServer(OvsdbClient ovsdbClient, OpensyncAPConfig opensyncAPConfig) {
        try {
            ApNetworkConfiguration apNetworkConfig = (ApNetworkConfiguration) opensyncAPConfig.getApProfile()
                    .getDetails();
            LOG.debug("configureNtpServer update Node_Config to {}", apNetworkConfig.getNtpServer().getValue());
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("value", new Atom<>(apNetworkConfig.getNtpServer().getValue()));
            List<Operation> operations = new ArrayList<>();
            operations.add(new Update(nodeConfigTable,
                    List.of(new Condition("module", Function.EQUALS, new Atom<>("ntp"))), new Row(updateColumns)));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult res : result) {
                LOG.debug("configureNtpServer result {}", res);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
