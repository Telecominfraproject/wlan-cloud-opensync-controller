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
import com.telecominfraproject.wlan.opensync.ovsdb.dao.models.RadiusConfigInfo;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.network.models.RadSecConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbRadSecConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet getProvisionedData;

    void configureApc(OvsdbClient ovsdbClient, Boolean enable, List<Operation> operations) {
        try {
            if (ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(apcConfigDbTable)) {
                Map<String, Value> updateColumns = new HashMap<>();
                updateColumns.put("enabled", new Atom<>(enable));
                Row row = new Row(updateColumns);
                Update update = new Update(apcConfigDbTable, row);
                if (!operations.contains(update)) {
                    // only need to do 1 update of this kind
                    operations.add(new Update(apcConfigDbTable, row));
                }
            }
        } catch (InterruptedException | ExecutionException | OvsdbClientException e) {
            LOG.error("Exception getting schema for ovsdb.", e);
            throw new RuntimeException(e);
        }
    }

    void configureRadiusAndRealm(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig) {
        List<Operation> operations = new ArrayList<>();
        try {
            if ((ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(realmConfigDbTable)
                    && ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(radiusConfigDbTable))) {
                configureRadiusServers(ovsdbClient, apConfig, operations);
                configureRealmForRadiusServers(ovsdbClient, apConfig);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Exception provisioning RadSecConfiguraitons.", e);
            throw new RuntimeException(e);
        }
    }

    private void configureRadiusServers(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig, List<Operation> operations)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {
        for (RadSecConfiguration rsc : ((ApNetworkConfiguration) apConfig.getApProfile().getDetails())
                .getRadSecConfigurations()) {
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("server", new Atom<>(rsc.getServer().getHostAddress()));
            updateColumns.put("client_cert", new Atom<>(externalFileStoreURL + rsc.getClientCert().getApExportUrl()));
            updateColumns.put("radius_config_name", new Atom<>(rsc.getName()));
            updateColumns.put("client_key", new Atom<>(externalFileStoreURL + rsc.getClientKey().getApExportUrl()));
            updateColumns.put("ca_cert", new Atom<>(externalFileStoreURL + rsc.getCaCert().getApExportUrl()));
            updateColumns.put("passphrase", new Atom<>(rsc.getPassphrase()));
            Row row = new Row(updateColumns);
            operations.add(new Insert(radiusConfigDbTable, row));
        }

        CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
        OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Insert into {}:", radiusConfigDbTable);
            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        }
    }

    private void configureRealmForRadiusServers(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {
        List<Operation> operations;
        CompletableFuture<OperationResult[]> fResult;
        OperationResult[] result;
        operations = new ArrayList<>();
        // now get the list of radius_configs, and build the realm config
        // table
        Map<String, RadiusConfigInfo> radiusConfigs = getProvisionedData.getProvisionedRadiusConfigs(ovsdbClient);
        for (RadSecConfiguration rsc : ((ApNetworkConfiguration) apConfig.getApProfile().getDetails())
                .getRadSecConfigurations()) {
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("server", new Atom<>(radiusConfigs.get(rsc.getName()).uuid));
            updateColumns.put("realm", new Atom<>(rsc.getRealm()));
            updateColumns.put("realm_config_name", new Atom<>(rsc.getName() + "_" + rsc.getRealm()));
            Row row = new Row(updateColumns);
            operations.add(new Insert(realmConfigDbTable, row));
        }

        fResult = ovsdbClient.transact(ovsdbName, operations);
        result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Insert into {}:", realmConfigDbTable);
            for (OperationResult res : result) {
                LOG.debug("Op Result {}", res);
            }
        }
    }

    void removeRadiusAndRealmConfigurations(OvsdbClient ovsdbClient) {
        LOG.info("removeRadiusAndRealmConfigurations from {} {}", radiusConfigDbTable, realmConfigDbTable);
        try {
            if ((ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(realmConfigDbTable)
                    && ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(radiusConfigDbTable))) {
                List<Operation> operations = new ArrayList<>();
                operations.add(new Delete(realmConfigDbTable));
                operations.add(new Delete(radiusConfigDbTable));
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                for (OperationResult res : result) {
                    LOG.info("Op Result {}", res);
                    if (res instanceof UpdateResult) {
                        LOG.info("removeRadiusAndRealmConfigurations {}", res.toString());
                    } else if (res instanceof ErrorResult) {
                        LOG.error("removeRadiusAndRealmConfigurations error {}", (res));
                        throw new RuntimeException("removeRadiusAndRealmConfigurations "
                                + ((ErrorResult) res).getError() + " " + ((ErrorResult) res).getDetails());
                    }
                }
                LOG.info("Removed all radius and realm configurations");
            }
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeRadiusAndRealmConfigurations", e);
            throw new RuntimeException(e);
        }
    }

}
