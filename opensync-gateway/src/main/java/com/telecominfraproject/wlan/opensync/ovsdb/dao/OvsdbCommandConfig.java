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

import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Insert;
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
public class OvsdbCommandConfig extends OvsdbDaoBase {
    @Autowired
    OvsdbGet ovsdbGet;

    void configureCommands(OvsdbClient ovsdbClient, String command, Map<String, String> payload, Long delay,
            Long duration) {

        LOG.debug("OvsdbDao::configureCommands command {}, payload {}, delay {} duration {}", command, payload, delay,
                duration);

        List<Operation> operations = new ArrayList<>();
        Map<String, Value> commandConfigColumns = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition("command", Function.EQUALS, new Atom<>(command)));

        commandConfigColumns.put("command", new Atom<>(command));
        commandConfigColumns.put("payload", com.vmware.ovsdb.protocol.operation.notation.Map.of(payload));

        commandConfigColumns.put("delay", new Atom<>(delay));
        commandConfigColumns.put("duration", new Atom<>(duration));
        commandConfigColumns.put("timestamp", new Atom<>(System.currentTimeMillis()));
        Row row = new Row(commandConfigColumns);
        if (ovsdbGet.getProvisionedCommandConfigs(ovsdbClient).containsKey(command)) {
            operations.add(new Update(commandConfigDbTable, conditions, row));
        } else {
            operations.add(new Insert(commandConfigDbTable, row));
        }

        try {
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);

            LOG.debug("OvsdbDao::configureCommands successfully configured command {} for duration {} payload {}",
                    command, duration, payload);

            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("OvsdbDao::configureCommands failed.", e);
            throw new RuntimeException(e);

        }

    }

}
