package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.role.PortalUserRole;

public class CustomerEquipmentCommand extends CEGWBaseCommand {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1981617366942595011L;
	
	protected CustomerEquipmentCommand() {
    }

    protected CustomerEquipmentCommand(CEGWCommandType commandType, String equipmentQRCode, long equipmentId) {
        this(commandType, equipmentQRCode, equipmentId, null, null);
    }

    protected CustomerEquipmentCommand(CEGWCommandType commandType, String equipmentQRCode, long equipmentId,
            PortalUserRole userRole, final String userName) {
        super(commandType, equipmentQRCode, equipmentId, userRole, userName);
    }

}
