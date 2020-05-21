package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.equipment.models.ApElementConfiguration;
import com.telecominfraproject.wlan.equipment.models.Equipment;
import com.telecominfraproject.wlan.equipment.models.StateSetting;
import com.telecominfraproject.wlan.location.models.Location;
import com.telecominfraproject.wlan.location.models.LocationDetails;
import com.telecominfraproject.wlan.profile.models.Profile;
import com.telecominfraproject.wlan.profile.models.ProfileType;
import com.telecominfraproject.wlan.profile.network.models.ApNetworkConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration;
import com.telecominfraproject.wlan.profile.ssid.models.SsidConfiguration.SecureMode;
import com.telecominfraproject.wlan.routing.models.EquipmentGatewayRecord;
import com.telecominfraproject.wlan.routing.models.EquipmentRoutingRecord;

public class OpensyncAPConfig extends BaseJsonModel {

	private static final long serialVersionUID = 3917975477206236668L;

	private Equipment customerEquipment;
	private Profile apProfile;
	private List<Profile> ssidProfile;
	private Location equipmentLocation;
	private EquipmentRoutingRecord equipmentRouting;
	private EquipmentGatewayRecord equipmentGateway;

	// Handle Legacy Config Support
	public void setRadioConfig(OpensyncAPRadioConfig radioConfig) {

		if (customerEquipment == null) {
			customerEquipment = new Equipment();
			customerEquipment.setId(0);
			customerEquipment.setEquipmentType(EquipmentType.AP);
			customerEquipment.setDetails(ApElementConfiguration.createWithDefaults());
			ApElementConfiguration apConfig = (ApElementConfiguration) customerEquipment.getDetails();
			apConfig.getRadioMap().get(RadioType.is2dot4GHz).setChannelNumber(radioConfig.getRadioChannel24G());
			apConfig.getRadioMap().get(RadioType.is5GHzL).setChannelNumber(radioConfig.getRadioChannel5LG());
			apConfig.getRadioMap().get(RadioType.is5GHzU).setChannelNumber(radioConfig.getRadioChannel5HG());
			customerEquipment.setDetails(apConfig);
		}

		if (equipmentLocation == null) {
			equipmentLocation = new Location();
			equipmentLocation.setId(1);
			equipmentLocation.setDetails(LocationDetails.createWithDefaults());
			((LocationDetails) equipmentLocation.getDetails())
					.setCountryCode(CountryCode.getByName(radioConfig.getCountry().toLowerCase()));
			customerEquipment.setLocationId(equipmentLocation.getId());
		}

	}

	// Handle Legacy Config Support
	public void setSsidConfigs(List<OpensyncAPSsidConfig> ssidConfigs) {

		if (apProfile == null) {
			apProfile = new Profile();
			apProfile.setName("GeneratedApProfile");
			apProfile.setId(2);
			apProfile.setDetails(ApNetworkConfiguration.createWithDefaults());
		}

		long ssidProfileId = 3;
		for (OpensyncAPSsidConfig ssidConfig : ssidConfigs) {

			Profile profile = new Profile();
			profile.setProfileType(ProfileType.ssid);
			profile.setName(ssidConfig.getSsid());
			SsidConfiguration cfg = SsidConfiguration.createWithDefaults();
			Set<RadioType> appliedRadios = new HashSet<RadioType>();
			appliedRadios.add(ssidConfig.getRadioType());
			cfg.setAppliedRadios(appliedRadios);
			cfg.setSsid(ssidConfig.getSsid());
			if (ssidConfig.getEncryption().equals("WPA-PSK") && ssidConfig.getMode().equals("1"))
				cfg.setSecureMode(SecureMode.wpaPSK);
			else
				cfg.setSecureMode(SecureMode.wpa2PSK);
			cfg.setBroadcastSsid(ssidConfig.isBroadcast() ? StateSetting.enabled : StateSetting.disabled);

			profile.setDetails(cfg);
			profile.setId(ssidProfileId);
			if (this.ssidProfile == null)
				this.ssidProfile = new ArrayList<Profile>();
			this.ssidProfile.add(profile);
			apProfile.getChildProfileIds().add(ssidProfileId);
			ssidProfileId++;

		}

		if (customerEquipment != null) {
			customerEquipment.setProfileId(apProfile.getId());
		}

	}

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

	public List<Profile> getSsidProfile() {
		return ssidProfile;
	}

	public void setSsidProfile(List<Profile> ssidProfile) {
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
		if (ssidProfile != null) {
			List<Profile> ssidList = new ArrayList<Profile>();
			for (Profile profile : ssidProfile) {
				ssidList.add(profile.clone());
			}
			ret.ssidProfile = ssidList;
		}
		if (apProfile != null)
			ret.apProfile = apProfile.clone();
		if (equipmentRouting != null)
			ret.equipmentRouting = equipmentRouting.clone();
		if (equipmentGateway != null)
			ret.equipmentGateway = equipmentGateway.clone();

		return ret;
	}

}
