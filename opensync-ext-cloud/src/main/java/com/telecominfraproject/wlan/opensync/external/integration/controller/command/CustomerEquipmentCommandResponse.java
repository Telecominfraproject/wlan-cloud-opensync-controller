package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class CustomerEquipmentCommandResponse extends BaseJsonModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5977811650122183402L;
	
	/**
     * Constructor 
     * 
     * @param resultCode
     * @param resultDetail
     */
    public CustomerEquipmentCommandResponse(int resultCode, String resultDetail) {
        this.resultCode = resultCode;
        this.resultDetail = resultDetail;
    }

    public CustomerEquipmentCommandResponse() {
    }

    /**
     * Result code, use CEGWCommandResultCode to set it
     */
    private int resultCode;

    /**
     * Detail for the result
     */
    private String resultDetail;

    public void setResponseCode(int resulteCode) {
        this.resultCode = resulteCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    @Override
    public CustomerEquipmentCommandResponse clone() {
        return (CustomerEquipmentCommandResponse) super.clone();
    }

    public String getResultDetail() {
        return resultDetail;
    }

    public void setResultDetail(String resultDetail) {
        this.resultDetail = resultDetail;
    }

}
