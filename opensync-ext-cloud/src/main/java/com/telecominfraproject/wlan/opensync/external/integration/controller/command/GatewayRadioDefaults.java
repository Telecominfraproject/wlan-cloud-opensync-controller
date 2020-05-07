package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class GatewayRadioDefaults extends BaseJsonModel {
	/**
	* 
	*/
	private static final long serialVersionUID = 8302320644769648937L;
	private Boolean default11k;
	private Boolean default11v;
	private Boolean default11r;

	public GatewayRadioDefaults() {
		//
	}

	public Boolean getDefault11k() {
		return default11k;
	}

	public void setDefault11k(Boolean default11k) {
		this.default11k = default11k;
	}

	public Boolean getDefault11v() {
		return default11v;
	}

	public void setDefault11v(Boolean default11v) {
		this.default11v = default11v;
	}

	public Boolean getDefault11r() {
		return default11r;
	}

	public void setDefault11r(Boolean default11r) {
		this.default11r = default11r;
	}
}
