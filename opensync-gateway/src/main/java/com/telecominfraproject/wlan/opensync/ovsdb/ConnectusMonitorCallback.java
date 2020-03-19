package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.opensync.ovsdb.dao.OvsdbDao;
import com.vmware.ovsdb.callback.MonitorCallback;
import com.vmware.ovsdb.protocol.methods.RowUpdate;
import com.vmware.ovsdb.protocol.methods.TableUpdates;
import com.vmware.ovsdb.protocol.operation.notation.Row;
import com.vmware.ovsdb.service.OvsdbClient;

public class ConnectusMonitorCallback implements MonitorCallback {

	private OvsdbClient ovsdbClient;
	private String connectedClientId;

	private static final Logger LOG = LoggerFactory.getLogger(ConnectusMonitorCallback.class);

	public ConnectusMonitorCallback(OvsdbClient ovsdbClient, String clientId) {
		this.ovsdbClient = ovsdbClient;
		this.connectedClientId = clientId;
	}

	@Override
	public void update(TableUpdates tableUpdates) {
		for (String key : tableUpdates.getTableUpdates().keySet()) {

			LOG.debug("Received update to table {} on AP {}", key,
					connectedClientId);
			
			if (key.equalsIgnoreCase(OvsdbDao.wifiInetStateDbTable)) {
				Map<UUID, RowUpdate> rowUpdates = tableUpdates.getTableUpdates().get(OvsdbDao.wifiInetStateDbTable)
						.getRowUpdates();

				for (UUID rowId : rowUpdates.keySet()) {

					logRow(rowUpdates, rowId);

				}

			} else if (key.equalsIgnoreCase(OvsdbDao.wifiRadioStateDbTable)) {
				Map<UUID, RowUpdate> rowUpdates = tableUpdates.getTableUpdates().get(OvsdbDao.wifiRadioStateDbTable)
						.getRowUpdates();

				for (UUID rowId : rowUpdates.keySet()) {

					logRow(rowUpdates, rowId);

				}

			} else if (key.equals(OvsdbDao.wifiVifStateDbTable)) {
				Map<UUID, RowUpdate> rowUpdates = tableUpdates.getTableUpdates().get(OvsdbDao.wifiVifStateDbTable)
						.getRowUpdates();

				for (UUID rowId : rowUpdates.keySet()) {

					logRow(rowUpdates, rowId);

				}

			}
		}
	}

	private void logRow(Map<UUID, RowUpdate> rowUpdates, UUID rowId) {
		RowUpdate rowUpdate = rowUpdates.get(rowId);
		Row oldRow = rowUpdate.getOld();
		oldRow.getColumns().entrySet().stream()
				.forEach(e -> LOG.trace("Key {} Value {}", e.getKey(), e.getValue().toString()));
		Row newRow = rowUpdate.getNew();
		newRow.getColumns().entrySet().stream()
				.forEach(e -> LOG.trace("Key {} Value {}", e.getKey(), e.getValue().toString()));
	}

}