
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
import com.vmware.ovsdb.protocol.operation.notation.Atom;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.protocol.operation.notation.Set;
import com.vmware.ovsdb.protocol.operation.notation.Value;
import com.vmware.ovsdb.protocol.operation.result.ErrorResult;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.operation.result.UpdateResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;

@Component
public class OvsdbRadiusProxyConfig extends OvsdbDaoBase {

    @Autowired
    OvsdbGet getProvisionedData;

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

    /*
     * root@OpenAp-ab1f4d:~# ovsdb-client list-columns Radius_Proxy_Config
     * 
     * Column Type
     * ------------------ ---------------------------------------------------------------
     * realm {"key":{"maxLength":256,"type":"string"},"max":16,"min":0}
     * radius_config_name "string"
     * _uuid "uuid"
     * acct_port {"key":"integer","min":0}
     * client_key {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
     * server "string"
     * _version "uuid"
     * port "integer"
     * radsec "boolean"
     * client_cert {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
     * acct_server {"key":"string","min":0}
     * passphrase {"key":{"maxLength":128,"type":"string"},"min":0}
     * acct_secret {"key":"string","min":0}
     * ca_cert {"key":{"maxLength":256,"minLength":1,"type":"string"},"min":0}
     * auto_discover "boolean"
     * secret "string"
     */
    private void configureRadiusServers(OvsdbClient ovsdbClient, OpensyncAPConfig apConfig, List<Operation> operations)
            throws OvsdbClientException, InterruptedException, ExecutionException, TimeoutException {

        // TODO: remove the schema check when AP load available
        DatabaseSchema databaseSchema = ovsdbClient.getSchema(ovsdbName).get();

        for (RadiusProxyConfiguration rsc : ((ApNetworkConfiguration) apConfig.getApProfile().getDetails()).getRadiusProxyConfigurations()) {
            Map<String, Value> updateColumns = new HashMap<>();
            if (rsc.getServer() != null)
                updateColumns.put("server", new Atom<>(rsc.getServer().getHostAddress()));
            if (rsc.getName() != null)
                updateColumns.put("radius_config_name", new Atom<>(rsc.getName()));
            if (rsc.getPort() != null)
                updateColumns.put("port", new Atom<>(rsc.getPort()));
            if (rsc.getRealm() != null)
                updateColumns.put("realm", Set.of(rsc.getRealm()));
            if (rsc.getUseRadSec() != null) {
                updateColumns.put("radsec", new Atom<>(rsc.getUseRadSec()));
                if (rsc.getUseRadSec()) {
                    getCertificateUrls(rsc, updateColumns);
                    updateColumns.put("passphrase", new Atom<>(rsc.getPassphrase()));
                }
            }
            if (rsc.getSharedSecret() != null)
                updateColumns.put("secret", new Atom<>(rsc.getSharedSecret()));
            if (rsc.getAcctServer() != null) {
                updateColumns.put("acct_server", new Atom<>(rsc.getAcctServer().getHostAddress()));
            }
            if (rsc.getSharedSecret() != null) {
                updateColumns.put("acct_secret", new Atom<>(rsc.getAcctSharedSecret()));
            }
            if (rsc.getAcctPort() != null) {
                updateColumns.put("acct_port", new Atom<>(rsc.getAcctPort()));
            }
            if (rsc.getRadiusProxySecret() != null) {
                updateColumns.put("proxy_secret", new Atom<>(rsc.getRadiusProxySecret()));
            }
            if (databaseSchema.getTables().get(radiusConfigDbTable).getColumns().containsKey("auto_discover")) {
                if (rsc.getUseRadSec() != null && rsc.getUseRadSec() && rsc.getDynamicDiscovery() != null &&  rsc.getDynamicDiscovery()) {
                    // if useRadSec && dynamicDiscovery enabled, do not send server information
                    updateColumns.put("auto_discover", new Atom<>(true));
                    updateColumns.remove("acct_server");
                    updateColumns.remove("acct_secret");
                    updateColumns.remove("acct_port");
                    updateColumns.remove("server");
                    updateColumns.remove("port");
                    updateColumns.remove("secret");
                } else {
                    // if !useRadSec, auto_discover is false regardless of it's desired setting
                    // retain server information
                    updateColumns.put("auto_discover", new Atom<>(false));
                }
            }
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
                        throw new RuntimeException("removeRadiusConfigurations " + ((ErrorResult) res).getError() + " " + ((ErrorResult) res).getDetails());
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
