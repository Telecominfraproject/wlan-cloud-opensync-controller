package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

public class CEGWBaseCommand extends BaseJsonModel {
	 /**
	 * 
	 */
	private static final long serialVersionUID = 1369966637498613810L;

	/**
     * Command Type
     */
    private CEGWCommandType commandType;

    /**
     * Equipment Identifier
     */
    private long equipmentId;

    /**
     * Equipment QR identifier
     */
    private String equipmentQRCode;
    /**
     * Portal user name
     */
    private String userName;

    /**
     * Portal user role
     */
    private PortalUserRole userRole;

    /**
     * Only used to JSON decoding
     */
    protected CEGWBaseCommand() {
        this.commandType = CEGWCommandType.Unknown;
    }

    protected CEGWBaseCommand(CEGWCommandType commandType, String equipmentQRCode, long equipmentId) {
        this(commandType, equipmentQRCode, equipmentId, null, null);
    }

    protected CEGWBaseCommand(CEGWCommandType commandType, String equipmentQRCode, long equipmentId,
            PortalUserRole userRole, String userName) {
        this.commandType = commandType;
        this.equipmentQRCode = equipmentQRCode;
        this.equipmentId = equipmentId;
        this.userRole = userRole;
        this.userName = userName;
    }

    @Override
    public CEGWBaseCommand clone() {
        return (CEGWBaseCommand) super.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CEGWBaseCommand)) {
            return false;
        }
        CEGWBaseCommand other = (CEGWBaseCommand) obj;
        if (this.commandType != other.commandType) {
            return false;
        }
        if (this.equipmentId != other.equipmentId) {
            return false;
        }
        if (this.equipmentQRCode == null) {
            if (other.equipmentQRCode != null) {
                return false;
            }
        } else if (!this.equipmentQRCode.equals(other.equipmentQRCode)) {
            return false;
        }
        if (this.userRole != other.userRole) {
            return false;
        }
        if (this.userName == null) {
            if (other.userName != null) {
                return false;
            }
        } else if (!this.userName.equals(other.userName)) {
            return false;
        }
        return true;
    }

    public CEGWCommandType getCommandType() {
        return commandType;
    }

    public long getEquipmentId() {
        return this.equipmentId;
    }

    public String getEquipmentQRCode() {
        return equipmentQRCode;
    }

    public String getUsername() {
        return userName;
    }

    public PortalUserRole getUserRole() {
        return userRole;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.commandType == null) ? 0 : this.commandType.hashCode());
        result = prime * result + (int) (this.equipmentId ^ (this.equipmentId >>> 32));
        result = prime * result + ((this.equipmentQRCode == null) ? 0 : this.equipmentQRCode.hashCode());
        result = prime * result + ((this.userRole == null) ? 0 : this.userRole.hashCode());
        result = prime * result + ((this.userName == null) ? 0 : this.userName.hashCode());
        return result;
    }

    @Override
    public boolean hasUnsupportedValue() {
        if (super.hasUnsupportedValue()) {
            return true;
        }

        if (CEGWCommandType.isUnsupported(commandType) || PortalUserRole.isUnsupported(userRole)) {
            return true;
        }
        return false;
    }

    public void setCommandType(CEGWCommandType commandType) {
        this.commandType = commandType;
    }

    public void setEquipmentId(long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public void setEquipmentQRCode(String equipmentQRCode) {
        this.equipmentQRCode = equipmentQRCode;
    }

    public void setUsername(String portalUsername) {
        this.userName = portalUsername;
    }

    public void setUserRole(PortalUserRole portalRole) {
        this.userRole = portalRole;
    }

}
