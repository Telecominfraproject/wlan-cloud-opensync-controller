package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

public class CEGWConfigChangeNotification extends CustomerEquipmentCommand {


    /**
	 * 
	 */
	private static final long serialVersionUID = 4401284478686864193L;

	/**
     * Constructor
     * 
     * @param equipementQRCode
     * @param equipmentId
     */
    public CEGWConfigChangeNotification(String equipementQRCode, long equipmentId) {
        super(CEGWCommandType.ConfigChangeNotification, equipementQRCode, equipmentId);
    }

    /**
     * Constructor used by JSON
     */
    protected CEGWConfigChangeNotification() {
        super(CEGWCommandType.ConfigChangeNotification, null, 0);
    }
}
