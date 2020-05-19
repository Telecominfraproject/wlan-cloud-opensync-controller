package com.telecominfraproject.wlan.opensync.external.integration.models;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.routing.models.EquipmentGatewayRecord;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;

public class OpensyncAPConfig extends BaseJsonModel {

	private static final long serialVersionUID = 3917975477206236668L;

	private Equipment customerEquipment;
	private Profile apProfile;
	private Profile ssidProfile;
	private Location equipmentLocation;
	private EquipmentRoutingRecord equipmentRouting;
	private EquipmentGatewayRecord equipmentGateway;

	public EquipmentGatewayRecord getEquipmentGateway() {
		return equipmentGateway;
	}

	public void setEquipmentGateway(EquipmentGatewayRecord equipmentGateway) {
		this.equipmentGateway = equipmentGateway;
	}

	public EquipmentRoutingRecord getEquipmentRouting() {
		return equipmentRouting;
	}

	public void setEquipmentRouting(EquipmentRoutingRecord equipmentRouting) {
		this.equipmentRouting = equipmentRouting;
	}

	public Equipment getCustomerEquipment() {
		return customerEquipment;
	}

	public void setCustomerEquipment(Equipment customerEquipment) {
		this.customerEquipment = customerEquipment;
	}

	public Profile getApProfile() {
		return apProfile;
	}

	public void setApProfile(Profile apProfile) {
		this.apProfile = apProfile;
	}

	public Profile getSsidProfile() {
		return ssidProfile;
	}

	public void setSsidProfile(Profile ssidProfile) {
		this.ssidProfile = ssidProfile;
	}

	public Location getEquipmentLocation() {
		return equipmentLocation;
	}

	public void setEquipmentLocation(Location equipmentLocation) {
		this.equipmentLocation = equipmentLocation;
	}

	public String getCountryCode() {
		return Location.getCountryCode(this.equipmentLocation).toString();
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public OpensyncAPConfig clone() {
		OpensyncAPConfig ret = (OpensyncAPConfig) super.clone();

		if (customerEquipment != null)
			ret.customerEquipment = customerEquipment.clone();
		if (equipmentLocation != null)
			ret.equipmentLocation = equipmentLocation.clone();
		if (ssidProfile != null)
			ret.ssidProfile = ssidProfile.clone();
		if (apProfile != null)
			ret.apProfile = apProfile.clone();
		if (equipmentRouting != null)
			ret.equipmentRouting = equipmentRouting.clone();
		if (equipmentGateway != null)
			ret.equipmentGateway = equipmentGateway.clone();

		return ret;
	}

}
