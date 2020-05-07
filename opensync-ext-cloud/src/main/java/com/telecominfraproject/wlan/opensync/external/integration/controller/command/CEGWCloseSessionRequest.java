package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

public class CEGWCloseSessionRequest extends CEGatewayCommand {


    /**
	 * 
	 */
	private static final long serialVersionUID = -263965970528271895L;

	public CEGWCloseSessionRequest(String qrCode, long equipmentId) {
        super(CEGWCommandType.CloseSessionRequest, qrCode, equipmentId);
    }

    /**
     * Constructor used by JSON
     */
    public CEGWCloseSessionRequest() {
        super(CEGWCommandType.CloseSessionRequest, null, 0);
    }

    @Override
    public CEGWCloseSessionRequest clone() {
        return (CEGWCloseSessionRequest) super.clone();
    }

}
