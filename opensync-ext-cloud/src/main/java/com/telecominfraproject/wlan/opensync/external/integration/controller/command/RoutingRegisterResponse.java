package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class RoutingRegisterResponse extends BaseJsonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5832910847688930093L;

	/**
     * Created record
     */
    private EquipmentRoutingRecord routingRecord;

    /**
     * Target Service Deployment is optional. If set, getwaye should redirect
     * the Equipment.
     */
    private ServiceDeploymentInfo targetDeploymentInfo;

    /**
     * Constructor
     */
    public RoutingRegisterResponse() {

    }

    /**
     * Constructor
     * 
     * @param routingRecord
     * @param targetDeploymentInfo
     */
    public RoutingRegisterResponse(EquipmentRoutingRecord routingRecord, ServiceDeploymentInfo targetDeploymentInfo) {
        this.routingRecord = routingRecord;
        this.targetDeploymentInfo = targetDeploymentInfo;
    }

    @Override
    public RoutingRegisterResponse clone() {
        RoutingRegisterResponse result = (RoutingRegisterResponse) super.clone();
        if (null != this.routingRecord) {
            result.routingRecord = this.routingRecord.clone();
        }
        if (null != this.targetDeploymentInfo) {
            result.targetDeploymentInfo = this.targetDeploymentInfo.clone();
        }
        return result;
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
        if (!(obj instanceof RoutingRegisterResponse)) {
            return false;
        }
        RoutingRegisterResponse other = (RoutingRegisterResponse) obj;
        if (routingRecord == null) {
            if (other.routingRecord != null) {
                return false;
            }
        } else if (!routingRecord.equals(other.routingRecord)) {
            return false;
        }
        if (targetDeploymentInfo == null) {
            if (other.targetDeploymentInfo != null) {
                return false;
            }
        } else if (!targetDeploymentInfo.equals(other.targetDeploymentInfo)) {
            return false;
        }
        return true;
    }

    public EquipmentRoutingRecord getRoutingRecord() {
        return routingRecord;
    }

    public ServiceDeploymentInfo getTargetDeploymentInfo() {
        return targetDeploymentInfo;
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
        result = prime * result + ((routingRecord == null) ? 0 : routingRecord.hashCode());
        result = prime * result + ((targetDeploymentInfo == null) ? 0 : targetDeploymentInfo.hashCode());
        return result;
    }

    public void setRoutingRecord(EquipmentRoutingRecord routingRecord) {
        this.routingRecord = routingRecord;
    }

    public void setTargetDeploymentInfo(ServiceDeploymentInfo targetDeploymentInfo) {
        this.targetDeploymentInfo = targetDeploymentInfo;
    }
    
    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }

        if (hasUnsupportedValue(routingRecord) || hasUnsupportedValue(targetDeploymentInfo)) {
            return true;
        }
        return false;
    }
}
