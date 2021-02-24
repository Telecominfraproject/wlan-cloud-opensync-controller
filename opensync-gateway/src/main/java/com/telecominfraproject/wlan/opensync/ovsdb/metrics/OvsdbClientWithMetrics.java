package com.telecominfraproject.wlan.opensync.ovsdb.metrics;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.vmware.ovsdb.callback.LockCallback;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.exception.OvsdbClientException;
import com.vmware.ovsdb.protocol.methods.LockResult;
import com.vmware.ovsdb.protocol.methods.MonitorRequests;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.Operation;
import com.vmware.ovsdb.protocol.operation.result.OperationResult;
import com.vmware.ovsdb.protocol.schema.DatabaseSchema;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.OvsdbConnectionInfo;

public class OvsdbClientWithMetrics implements OvsdbClient {
    
    private final OvsdbMetrics metrics; 
    private final OvsdbClient delegate;

    public OvsdbClientWithMetrics(OvsdbClient delegate, OvsdbMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    public CompletableFuture<String[]> listDatabases() throws OvsdbClientException {
        metrics.listDatabases.increment();
        return delegate.listDatabases();
    }

    public CompletableFuture<DatabaseSchema> getSchema(String dbName) throws OvsdbClientException {
        metrics.getSchema.increment();
        return delegate.getSchema(dbName);
    }

    public CompletableFuture<OperationResult[]> transact(String dbName, List<Operation> operations)
            throws OvsdbClientException {
        metrics.transact.increment();
        return delegate.transact(dbName, operations);
    }

    public CompletableFuture<TableUpdates> monitor(String dbName, String monitorId, MonitorRequests monitorRequests,
            MonitorCallback monitorCallback) throws OvsdbClientException {
        metrics.monitor.increment();        
        return delegate.monitor(dbName, monitorId, monitorRequests, monitorCallback);
    }

    public CompletableFuture<Void> cancelMonitor(String monitorId) throws OvsdbClientException {
        metrics.cancelMonitor.increment();        
        return delegate.cancelMonitor(monitorId);
    }

    public CompletableFuture<LockResult> lock(String lockId, LockCallback lockCallback) throws OvsdbClientException {
        metrics.lock.increment();        
        return delegate.lock(lockId, lockCallback);
    }

    public CompletableFuture<LockResult> steal(String lockId, LockCallback lockCallback) throws OvsdbClientException {
        metrics.steal.increment();        
        return delegate.steal(lockId, lockCallback);
    }

    public CompletableFuture<Void> unlock(String lockId) throws OvsdbClientException {
        metrics.unlock.increment();        
        return delegate.unlock(lockId);
    }

    public OvsdbConnectionInfo getConnectionInfo() {
        return delegate.getConnectionInfo();
    }

    public void shutdown() {
        delegate.shutdown();
    }    

}
