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
import com.telecominfraproject.wlan.profile.network.models.RadiusProxyConfiguration;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.operation.Delete;
import com.vmware.ovsdb.protocol.operation.Insert;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.Update;
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Set;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbRadiusProxyConfig extends OvsdbDaoBase {

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

    void configureRadius(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig) {
        List<Operation> operations = new ArrayList<>();
        try {
            if (ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(radiusConfigDbTable)) {
                configureRadiusServers(ovsdbClient, apConfig, operations);
            }
        } catch (OvsdbClientException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Exception provisioning RadSecConfiguraitons.", e);
            throw new RuntimeException(e);
        }
    }

    private void configureRadiusServers(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig, List<Operation> operations)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {

        for (RadiusProxyConfiguration rsc : ((ApNetworkConfiguration) apConfig.getApProfile().getDetails())
                .getRadiusProxyConfigurations()) {
            Map<String, Value> updateColumns = new HashMap<>();
            updateColumns.put("server", new Atom<>(rsc.getServer().getHostAddress()));
            getCertificateUrls(rsc, updateColumns);
            updateColumns.put("radius_config_name", new Atom<>(rsc.getName()));
            updateColumns.put("passphrase", new Atom<>(rsc.getPassphrase()));          
            updateColumns.put("port", new Atom<>(rsc.getPort()));           
            updateColumns.put("realm", Set.of(rsc.getRealm()));
            updateColumns.put("radsec", new Atom<>(rsc.getUseRadSec()));
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

    private void getCertificateUrls(RadiusProxyConfiguration rsc, Map<String, Value> updateColumns) {
        String clientCertFilestoreUrl = externalFileStoreURL + rsc.getClientCert().getApExportUrl();
        String clientKeyFilestoreUrl = externalFileStoreURL + rsc.getClientKey().getApExportUrl();
        String caCertFilestoreUrl = externalFileStoreURL + rsc.getCaCert().getApExportUrl();
        if (!clientCertFilestoreUrl.contains("filestore")) {
            clientCertFilestoreUrl = externalFileStoreURL + "/filestore/" + rsc.getClientCert().getApExportUrl();
        }
        if (!clientKeyFilestoreUrl.contains("filestore")) {
            clientKeyFilestoreUrl = externalFileStoreURL + "/filestore/" + rsc.getClientKey().getApExportUrl();
        }
        if (!caCertFilestoreUrl.contains("filestore")) {
            caCertFilestoreUrl = externalFileStoreURL + "/filestore/" + rsc.getCaCert().getApExportUrl();
        }           
        updateColumns.put("client_cert", new Atom<>(clientCertFilestoreUrl));
        updateColumns.put("client_key", new Atom<>(clientKeyFilestoreUrl));
        updateColumns.put("ca_cert", new Atom<>(caCertFilestoreUrl));
    }

    void removeRadiusConfigurations(OvsdbClient ovsdbClient) {
        LOG.info("removeRadiusConfigurations from {}", radiusConfigDbTable);
        try {
            if (ovsdbClient.getSchema(ovsdbName).get().getTables().containsKey(radiusConfigDbTable)) {
                List<Operation> operations = new ArrayList<>();
                operations.add(new Delete(radiusConfigDbTable));
                CompletableFuture<OperationResult[]> fResult = ovsdbClient.transact(ovsdbName, operations);
                OperationResult[] result = fResult.get(ovsdbTimeoutSec, TimeUnit.SECONDS);
                for (OperationResult res : result) {
                    LOG.info("Op Result {}", res);
                    if (res instanceof UpdateResult) {
                        LOG.info("removeRadiusConfigurations {}", res.toString());
                    } else if (res instanceof ErrorResult) {
                        LOG.error("removeRadiusConfigurations error {}", (res));
                        throw new RuntimeException("removeRadiusConfigurations "
                                + ((ErrorResult) res).getError() + " " + ((ErrorResult) res).getDetails());
                    }
                }
                LOG.info("Removed all radius and realm configurations");
            }
        } catch (OvsdbClientException | TimeoutException | ExecutionException | InterruptedException e) {
            LOG.error("Error in removeRadiusConfigurations", e);
            throw new RuntimeException(e);
        }
    }

}
