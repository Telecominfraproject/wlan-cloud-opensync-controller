package com.telecominfraproject.wlan.opensync.ovsdb.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbFirmwareConfig extends OvsdbDaoBase {

    void configureFirmwareDownload(OvsdbClient ovsdbClient, String apId, String firmwareUrl, String firmwareVersion,
            String username) throws Exception {
        try {
            LOG.debug("configureFirmwareDownload for {} to version {} url {} username {}", apId,
                    firmwareVersion, firmwareUrl, username);
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("upgrade_dl_timer", new Atom<>(upgradeDlTimerSeconds));
            updateColumns.put("firmware_url", new Atom<>(firmwareUrl));
            updateColumns.put("upgrade_timer", new Atom<>(upgradeTimerSeconds));
            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }
        } catch (Exception e) {
            LOG.error("Could not download firmware {} to AP {}", firmwareVersion, apId, e);
            throw new RuntimeException(e);
        }
    }

    void configureFirmwareFlash(OvsdbClient ovsdbClient, String apId, String firmwareVersion, String username) {
        try {
            LOG.debug("configureFirmwareFlash on AP {} to load {} setting timer for {} seconds.", apId, firmwareVersion,
                    upgradeTimerSeconds);
            List<Operation> operations = new ArrayList<>();
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("upgrade_timer", new Atom<>(upgradeTimerSeconds));
            Row row = new Row(updateColumns);
            operations.add(new Update(awlanNodeDbTable, row));
            CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
            OperationResult[] result = fResult.join();
            for (OperationResult r : result) {
                LOG.debug("Op Result {}", r);
            }
        } catch (Exception e) {
            LOG.error("Could not configure timer for flashing firmware {} on AP {}", firmwareVersion, apId, e);
            throw new RuntimeException(e);
        }
    }

}
