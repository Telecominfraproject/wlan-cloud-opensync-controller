package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class CustomerEquipmentGwRecord extends BaseJsonModel {
	private long id;

	////////////////////////////////////////////////////////////////////////////
	// If more parameters are required in this object, try to maintain the order
	// to match the DAO implementation.
	////////////////////////////////////////////////////////////////////////////

	/** Unique identifier for the CustomerEquipmentGW */
	private String gatewayId;

	/** IP Address of the CustomerEquipmentGW */
	private String ipAddr;

	/** Port for the CustomerEquipmentGW */
	private int port;

	/**
	 * Deployment Id for the gateway
	 */
	private int deploymentId;
	////////////////////////////////////////////////////////////////////////////

	private long createdTimestamp;
	private long lastModifiedTimestamp;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getCreatedTimestamp() {
		return createdTimestamp;
	}

	public void setDeploymentId(int deploymentId) {
		this.deploymentId = deploymentId;
	}

	public int getDeploymentId() {
		return this.deploymentId;
	}

	public void setCreatedTimestamp(long createdTimestamp) {
		this.createdTimestamp = createdTimestamp;
	}

	public long getLastModifiedTimestamp() {
		return lastModifiedTimestamp;
	}

	public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
		this.lastModifiedTimestamp = lastModifiedTimestamp;
	}

	@Override
	public CustomerEquipmentGwRecord clone() {
		return (CustomerEquipmentGwRecord) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (this.createdTimestamp ^ (this.createdTimestamp >>> 32));
		result = prime * result + this.deploymentId;
		result = prime * result + ((this.gatewayId == null) ? 0 : this.gatewayId.hashCode());
		result = prime * result + (int) (this.id ^ (this.id >>> 32));
		result = prime * result + ((this.ipAddr == null) ? 0 : this.ipAddr.hashCode());
		result = prime * result + (int) (this.lastModifiedTimestamp ^ (this.lastModifiedTimestamp >>> 32));
		result = prime * result + this.port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CustomerEquipmentGwRecord)) {
			return false;
		}
		CustomerEquipmentGwRecord other = (CustomerEquipmentGwRecord) obj;
		if (this.createdTimestamp != other.createdTimestamp) {
			return false;
		}
		if (this.deploymentId != other.deploymentId) {
			return false;
		}
		if (this.gatewayId == null) {
			if (other.gatewayId != null) {
				return false;
			}
		} else if (!this.gatewayId.equals(other.gatewayId)) {
			return false;
		}
		if (this.id != other.id) {
			return false;
		}
		if (this.ipAddr == null) {
			if (other.ipAddr != null) {
				return false;
			}
		} else if (!this.ipAddr.equals(other.ipAddr)) {
			return false;
		}
		if (this.lastModifiedTimestamp != other.lastModifiedTimestamp) {
			return false;
		}
		if (this.port != other.port) {
			return false;
		}
		return true;
	}
}
