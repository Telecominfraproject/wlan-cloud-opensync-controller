package com.telecominfraproject.wlan.opensync.external.integration.controller.command;

import java.util.HashMap;
import java.util.Map;

import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;

public class GatewayDefaults extends BaseJsonModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1781633756875649610L;
	private Map<RadioType, GatewayRadioDefaults> defaults = new HashMap<RadioType, GatewayRadioDefaults>();
	private Boolean default11w;

	public GatewayDefaults() {

	}

	public Map<RadioType, GatewayRadioDefaults> getDefaults() {
		return defaults;
	}

	public void setDefaults(Map<RadioType, GatewayRadioDefaults> defaults) {
		this.defaults = defaults;
	}

	public Boolean getDefault11w() {
		return default11w;
	}

	public void setDefault11w(Boolean default11w) {
		this.default11w = default11w;
	}
}
