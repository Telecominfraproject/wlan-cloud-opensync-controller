package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.service.GatewayType;

public class ServiceDeploymentInfo extends BaseJsonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3075868930647536783L;

	/**
     * Deployment Identifier
     */
    private int deploymentId;

    /**
     * Type of cloud service
     */
    private GatewayType serviceType;
    /**
     * External host name
     */
    private String serviceHostname;
    /**
     * External port
     */
    private int servicePort;
    /**
     * Last time record is modified
     */
    private long lastModifiedTimestamp;

    public ServiceDeploymentInfo() {
    }

    /**
     * Constructor
     * 
     * @param serivceType
     * @param deploymentId
     * @param serviceHostname
     * @param servicePort
     */
    public ServiceDeploymentInfo(GatewayType serivceType, int deploymentId, String serviceHostname, int servicePort) {
        this.serviceType = serivceType;
        this.deploymentId = deploymentId;
        this.serviceHostname = serviceHostname;
        this.servicePort = servicePort;
    }

    @Override
    public ServiceDeploymentInfo clone() {
        return (ServiceDeploymentInfo) super.clone();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ServiceDeploymentInfo)) {
            return false;
        }
        ServiceDeploymentInfo other = (ServiceDeploymentInfo) obj;
        if (deploymentId != other.deploymentId) {
            return false;
        }
        if (serviceHostname == null) {
            if (other.serviceHostname != null) {
                return false;
            }
        } else if (!serviceHostname.equals(other.serviceHostname)) {
            return false;
        }
        if (servicePort != other.servicePort) {
            return false;
        }
        if (serviceType != other.serviceType) {
            return false;
        }
        return true;
    }

    public int getDeploymentId() {
        return deploymentId;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public String getServiceHostname() {
        return serviceHostname;
    }

    public int getServicePort() {
        return servicePort;
    }

    public GatewayType getServiceType() {
        return serviceType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + deploymentId;
        result = prime * result + ((serviceHostname == null) ? 0 : serviceHostname.hashCode());
        result = prime * result + servicePort;
        result = prime * result + ((serviceType == null) ? 0 : serviceType.hashCode());
        return result;
    }

    public void setDeploymentId(int deploymentId) {
        this.deploymentId = deploymentId;
    }

    public void setLastModifiedTimestamp(long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public void setServiceHostname(String serviceHostname) {
        this.serviceHostname = serviceHostname;
    }

    public void setServicePort(int servicePort) {
        this.servicePort = servicePort;
    }

    public void setServiceType(GatewayType serviceType) {
        this.serviceType = serviceType;
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }
        if (GatewayType.isUnsupported(serviceType)) {
            return true;
        }
        return false;
    }
}
