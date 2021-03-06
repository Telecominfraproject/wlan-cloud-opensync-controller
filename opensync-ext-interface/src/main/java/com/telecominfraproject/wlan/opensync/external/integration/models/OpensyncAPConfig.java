package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.entity.CountryCode;
import com.telecominfraproject.wlan.core.model.equipment.EquipmentType;
import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
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

public class OpensyncAPConfig extends OpensyncAPBase {

    private static final long serialVersionUID = 3917975477206236668L;

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    private Equipment customerEquipment;
    private OpensyncAPHotspot20Config hotspotConfig;
    private Profile apProfile;
    private Profile rfProfile;
    private List<Profile> ssidProfile;
    private List<Profile> metricsProfile;
    private List<Profile> radiusProfiles;
    private Location equipmentLocation;
    private EquipmentRoutingRecord equipmentRouting;
    private EquipmentGatewayRecord equipmentGateway;
    private List<Profile> captiveProfiles;
    private List<Profile> bonjourGatewayProfiles;

    private List<MacAddress> blockedClients;

    @Override
    public OpensyncAPConfig clone() {
        OpensyncAPConfig ret = (OpensyncAPConfig) super.clone();

        if (customerEquipment != null) {
            ret.customerEquipment = customerEquipment.clone();
        }
        if (hotspotConfig != null) {
            ret.hotspotConfig = hotspotConfig.clone();
        }
        if (equipmentLocation != null) {
            ret.equipmentLocation = equipmentLocation.clone();
        }
        if (ssidProfile != null) {
            List<Profile> ssidList = new ArrayList<Profile>();
            for (Profile profile : ssidProfile) {
                ssidList.add(profile.clone());
            }
            ret.ssidProfile = ssidList;
        }
        if (metricsProfile != null) {
            List<Profile> metricsList = new ArrayList<Profile>();
            for (Profile profile : metricsProfile) {
                metricsList.add(profile.clone());
            }
            ret.metricsProfile = metricsList;
        }
        if (bonjourGatewayProfiles != null) {
            List<Profile> bonjourGatewayProfilesList = new ArrayList<Profile>();
            for (Profile profile : bonjourGatewayProfiles) {
                bonjourGatewayProfilesList.add(profile.clone());
            }
            ret.bonjourGatewayProfiles = bonjourGatewayProfilesList;
        }
        if (apProfile != null) {
            ret.apProfile = apProfile.clone();
        }
        if (rfProfile != null) {
            ret.rfProfile = rfProfile.clone();
        }
        if (equipmentRouting != null) {
            ret.equipmentRouting = equipmentRouting.clone();
        }
        if (equipmentGateway != null) {
            ret.equipmentGateway = equipmentGateway.clone();
        }
        if (radiusProfiles != null) {
            ret.radiusProfiles = new ArrayList<>();
            for (Profile radiusProfile : this.radiusProfiles) {
                ret.radiusProfiles.add(radiusProfile);
            }
        }
        if (captiveProfiles != null) {
            ret.captiveProfiles = new ArrayList<>();
            for (Profile cpConfig : this.captiveProfiles) {
                ret.captiveProfiles.add(cpConfig);
            }
        }
        if (blockedClients != null) {
            ret.blockedClients = new ArrayList<MacAddress>();
            for (MacAddress blockedClient : this.blockedClients) {
                ret.blockedClients.add(blockedClient);
            }
        }

        return ret;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpensyncAPConfig other = (OpensyncAPConfig) obj;
        return Objects.equals(apProfile, other.apProfile) && Objects.equals(blockedClients, other.blockedClients)
                && Objects.equals(bonjourGatewayProfiles, other.bonjourGatewayProfiles)
                && Objects.equals(captiveProfiles, other.captiveProfiles)
                && Objects.equals(customerEquipment, other.customerEquipment)
                && Objects.equals(equipmentGateway, other.equipmentGateway)
                && Objects.equals(equipmentLocation, other.equipmentLocation)
                && Objects.equals(equipmentRouting, other.equipmentRouting)
                && Objects.equals(hotspotConfig, other.hotspotConfig)
                && Objects.equals(metricsProfile, other.metricsProfile)
                && Objects.equals(radiusProfiles, other.radiusProfiles) && Objects.equals(rfProfile, other.rfProfile)
                && Objects.equals(ssidProfile, other.ssidProfile);
    }

    public Profile getApProfile() {
        return apProfile;
    }

    public List<MacAddress> getBlockedClients() {
        return blockedClients;
    }

    public List<Profile> getBonjourGatewayProfiles() {
        return bonjourGatewayProfiles;
    }

    public List<Profile> getCaptiveProfiles() {
        return captiveProfiles;
    }

    public String getCountryCode() {
        return Location.getCountryCode(this.equipmentLocation).toString();
    }

    public Equipment getCustomerEquipment() {
        return customerEquipment;
    }

    public EquipmentGatewayRecord getEquipmentGateway() {
        return equipmentGateway;
    }

    public Location getEquipmentLocation() {
        return equipmentLocation;
    }

    public EquipmentRoutingRecord getEquipmentRouting() {
        return equipmentRouting;
    }

    public OpensyncAPHotspot20Config getHotspotConfig() {
        return hotspotConfig;
    }

    public List<Profile> getMetricsProfiles() {
        return metricsProfile;
    }

    public List<Profile> getRadiusProfiles() {
        return radiusProfiles;
    }

    public Profile getRfProfile() {
        return rfProfile;
    }

    public List<Profile> getSsidProfile() {
        return ssidProfile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apProfile, blockedClients, bonjourGatewayProfiles, captiveProfiles, customerEquipment,
                equipmentGateway, equipmentLocation, equipmentRouting, hotspotConfig, metricsProfile, radiusProfiles,
                rfProfile, ssidProfile);
    }

    public void setApProfile(Profile apProfile) {
        this.apProfile = apProfile;
    }

    public void setBlockedClients(List<MacAddress> blockedClients) {
        this.blockedClients = blockedClients;
    }

    public void setBonjourGatewayProfiles(List<Profile> bonjourGatewayProfiles) {
        this.bonjourGatewayProfiles = bonjourGatewayProfiles;
    }

    public void setCaptiveProfiles(List<Profile> captiveProfiles) {
        this.captiveProfiles = captiveProfiles;
    }

    public void setCustomerEquipment(Equipment customerEquipment) {
        this.customerEquipment = customerEquipment;
    }

    public void setEquipmentGateway(EquipmentGatewayRecord equipmentGateway) {
        this.equipmentGateway = equipmentGateway;
    }

    public void setEquipmentLocation(Location equipmentLocation) {
        this.equipmentLocation = equipmentLocation;
    }

    public void setEquipmentRouting(EquipmentRoutingRecord equipmentRouting) {
        this.equipmentRouting = equipmentRouting;
    }

    public void setHotspotConfig(OpensyncAPHotspot20Config hotspotConfig) {
        this.hotspotConfig = hotspotConfig;
    }

    public void setMetricsProfiles(List<Profile> metricsProfileList) {
        metricsProfile = metricsProfileList;
    }

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
            equipmentLocation.getDetails()
                    .setCountryCode(CountryCode.getByName(radioConfig.getCountry().toLowerCase()));
            customerEquipment.setLocationId(equipmentLocation.getId());
        }

    }

    public void setRadiusProfiles(List<Profile> radiusProfiles) {
        this.radiusProfiles = radiusProfiles;
    }

    public void setRfProfile(Profile rfProfile) {
        this.rfProfile = rfProfile;
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
            if (ssidConfig.getEncryption().equals("WPA-PSK") && ssidConfig.getMode().equals("1")) {
                cfg.setSecureMode(SecureMode.wpaPSK);
            } else {
                cfg.setSecureMode(SecureMode.wpa2PSK);
            }
            cfg.setBroadcastSsid(ssidConfig.isBroadcast() ? StateSetting.enabled : StateSetting.disabled);

            profile.setDetails(cfg);
            profile.setId(ssidProfileId);
            if (this.ssidProfile == null) {
                this.ssidProfile = new ArrayList<Profile>();
            }
            this.ssidProfile.add(profile);
            apProfile.getChildProfileIds().add(ssidProfileId);
            ssidProfileId++;

        }

        if (customerEquipment != null) {
            customerEquipment.setProfileId(apProfile.getId());
        }

    }

    public void setSsidProfile(List<Profile> ssidProfile) {
        this.ssidProfile = ssidProfile;
    }

}
