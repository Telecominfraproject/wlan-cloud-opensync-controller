package com.telecominfraproject.wlan.opensync.external.integration.models;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.profile.models.Profile;

public class OpensyncAPHotspot20Config extends BaseJsonModel {

    private static final long serialVersionUID = -8495473152523219578L;
    
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    private Set<Profile> hotspot20ProfileSet;
    private Set<Profile> hotspot20OperatorSet;
    private Set<Profile> hotspot20VenueSet;
    private Set<Profile> hotspot20ProviderSet;


    
    public Set<Profile> getHotspot20ProfileSet() {
        return hotspot20ProfileSet;
    }
    
    public void setHotspot20ProfileSet(Set<Profile> hotspot20ProfileSet) {
        this.hotspot20ProfileSet = hotspot20ProfileSet;
    }

    public Set<Profile> getHotspot20OperatorSet() {
        return hotspot20OperatorSet;
    }
    
    public void setHotspot20OperatorSet(Set<Profile> hotspot20OperatorSet) {
        this.hotspot20OperatorSet = hotspot20OperatorSet;
    }
    
    public Set<Profile> getHotspot20VenueSet() {
        return hotspot20VenueSet;
    }
    
    public void setHotspot20VenueSet(Set<Profile> hotspot20VenueSet) {
        this.hotspot20VenueSet = hotspot20VenueSet;
    }

    public Set<Profile> getHotspot20ProviderSet() {
        return hotspot20ProviderSet;
    }
    
    public void setHotspot20ProviderSet(Set<Profile> hotspot20ProviderSet) {
        this.hotspot20ProviderSet = hotspot20ProviderSet;
    }

    @Override
    public OpensyncAPHotspot20Config clone() {
        OpensyncAPHotspot20Config ret = (OpensyncAPHotspot20Config) super.clone();

        if (hotspot20OperatorSet != null) {
            ret.hotspot20OperatorSet = new HashSet<Profile>(hotspot20OperatorSet);
        }

        if (hotspot20ProfileSet != null) {
            ret.hotspot20ProfileSet = new HashSet<Profile>(hotspot20ProfileSet);
        }

        if (hotspot20VenueSet != null) {
            ret.hotspot20VenueSet = new HashSet<Profile>(hotspot20VenueSet);
        }

        if (hotspot20ProviderSet != null) {
            ret.hotspot20ProviderSet = new HashSet<Profile>(hotspot20ProviderSet);
        }

        return ret;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hotspot20OperatorSet, hotspot20ProfileSet, hotspot20ProviderSet, hotspot20VenueSet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OpensyncAPHotspot20Config)) {
            return false;
        }
        OpensyncAPHotspot20Config other = (OpensyncAPHotspot20Config) obj;
        return Objects.equals(hotspot20OperatorSet, other.hotspot20OperatorSet)
                && Objects.equals(hotspot20ProfileSet, other.hotspot20ProfileSet)
                && Objects.equals(hotspot20ProviderSet, other.hotspot20ProviderSet)
                && Objects.equals(hotspot20VenueSet, other.hotspot20VenueSet);
    }

}
