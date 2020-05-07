package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

public class CEGatewayCommand extends CEGWBaseCommand {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4545056375390115226L;

	protected CEGatewayCommand() {
    }

    protected CEGatewayCommand(CEGWCommandType commandType, String equipmentQRCode, long equipmentId) {
        super(commandType, equipmentQRCode, equipmentId);
    }
}
