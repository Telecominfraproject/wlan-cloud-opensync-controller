package com.telecominfraproject.wlan.opensync.external.integration;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.telecominfraproject.wlan.core.model.equipment.MacAddress;
import com.telecominfraproject.wlan.core.model.equipment.RadioType;
import com.telecominfraproject.wlan.core.model.json.BaseJsonModel;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPInetState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPRadioState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAPVIFState;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncAWLANNode;
import com.telecominfraproject.wlan.opensync.external.integration.models.OpensyncWifiAssociatedClients;


public class OpensyncNode extends BaseJsonModel {
    private static final Logger LOG = LoggerFactory.getLogger(OpensyncNode.class);

    private static final long serialVersionUID = 8388356663505464850L;

    private String apId;
    private OpensyncAWLANNode awlanNode = null;
    private List<OpensyncAPRadioState> radioStates = null;
    private List<OpensyncAPInetState> inetStates = null;
    private List<OpensyncAPVIFState> vifStates = null;
    private List<OpensyncWifiAssociatedClients> wifiClients = null;
    private int customerId;
    private long equipmentId;

    public OpensyncNode(String apId, OpensyncAWLANNode awlanNode, int customerId, long equipmentId) {
        this.apId = apId;
        this.awlanNode = awlanNode;
        this.equipmentId = equipmentId;
        this.customerId = customerId;
        radioStates = new ArrayList<OpensyncAPRadioState>();
        inetStates = new ArrayList<OpensyncAPInetState>();
        vifStates = new ArrayList<OpensyncAPVIFState>();
        wifiClients = new ArrayList<OpensyncWifiAssociatedClients>();
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getApId() {
        return apId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public long getEquipmentId() {
        return equipmentId;
    }

    public void updateAWLANNode(OpensyncAWLANNode awlanNode) {
        this.awlanNode = awlanNode;
    }

    public OpensyncAWLANNode getAWLANNode() {
        return awlanNode;
    }

    public void updateRadioState(OpensyncAPRadioState radioState) {

        LOG.trace("Received Radio {}", radioState.toPrettyString());

        if (radioState.getIfName() == null || radioState.getFreqBand() == null)
            return; // not ready
        if (radioStates.isEmpty()) {
            radioStates.add(radioState);
        } else {
            for (OpensyncAPRadioState radio : radioStates) {
                if (radio.getFreqBand().equals(radioState.getFreqBand())
                        && radioState.getIfName().equals(radio.getIfName())) {
                    int index = radioStates.indexOf(radio);
                    radioStates.remove(index);
                    radioStates.add(radioState);
                    return;
                }
            }
            // no matching radio present in table
            radioStates.add(radioState);
        }

    }

    public void updateVifState(OpensyncAPVIFState vifState) {

        LOG.trace("Received {}", vifState.toPrettyString());
        if (vifState.getChannel() == -1 || vifState.getSsid() == null || vifState.getIfName() == null)
            return; // not ready

        if (vifStates.isEmpty()) {
            vifStates.add(vifState);
        } else {
            for (OpensyncAPVIFState vif : vifStates) {
                if (vifState.getSsid().equals(vif.getSsid()) && vifState.getIfName().equals(vif.getIfName())) {
                    int index = vifStates.indexOf(vif);
                    vifStates.remove(index);
                    vifStates.add(vifState);
                    return;
                }
            }
            // no matching radio present in table
            vifStates.add(vifState);
        }

    }

    public void updateInetState(OpensyncAPInetState inetState) {

        if (inetState.getHwAddr() == null || inetState.getIfName() == null)
            return;

        if (inetStates.isEmpty()) {
            inetStates.add(inetState);
        } else {
            for (OpensyncAPInetState inet : inetStates) {
                if ((inetState.getHwAddr() != null && inet.getHwAddr().equals(inetState.getHwAddr()))
                        || inet.getIfName().equals(inetState.getIfName())) {
                    int index = inetStates.indexOf(inet);
                    inetStates.set(index, inetState);
                    return;
                }
            }
            // no matching radio present in table
            inetStates.add(inetState);

        }
    }

    public void updateWifiClients(OpensyncWifiAssociatedClients wifiClient) {

        if (wifiClient.getMac() == null || wifiClient.getState() == null)
            return;

        if (wifiClients.isEmpty()) {
            wifiClients.add(wifiClient);
        } else {
            for (OpensyncWifiAssociatedClients client : wifiClients) {

                if (wifiClient.getMac().equals(client.getMac())) {

                    int index = wifiClients.indexOf(client);
                    wifiClients.remove(index);
                    wifiClients.add(wifiClient);
                    return;

                }

            }

            wifiClients.add(wifiClient);

        }
    }

    public OpensyncAWLANNode getAwlanNode() {
        return awlanNode;
    }

    public List<OpensyncAPRadioState> getRadioStates() {
        return radioStates;
    }

    public List<OpensyncAPInetState> getInetStates() {
        return inetStates;
    }

    public List<OpensyncAPVIFState> getVifStates() {
        return vifStates;
    }

    public List<OpensyncWifiAssociatedClients> getWifiClients() {
        return wifiClients;
    }

    public OpensyncAPRadioState getRadioForMac(MacAddress mac) {
        OpensyncAPRadioState ret = null;

        for (OpensyncAPRadioState radio : radioStates) {
            if (radio.getMac().equals(mac.toString())) {
                ret = radio;
                break;
            }
        }

        return ret;
    }

    public OpensyncAPRadioState getRadioForChannel(int channel) {
        OpensyncAPRadioState ret = null;

        for (OpensyncAPRadioState radio : radioStates) {
            if (radio.getChannel() == channel) {
                ret = radio;
                break;
            }
        }

        return ret;
    }

    public OpensyncAPRadioState getRadioForBand(String freq_band) {
        OpensyncAPRadioState ret = null;

        for (OpensyncAPRadioState radio : radioStates) {
            if (radio.getFreqBand().equals("5GL") && freq_band.equals(RadioType.is5GHz.toString())) {
                ret = radio;
                break;
            } else if (radio.getFreqBand().equals("2.4G") && freq_band.equals(RadioType.is2dot4GHz.toString())) {
                ret = radio;
                break;
            }
        }
        return ret;
    }

    public OpensyncAPVIFState getVIFForChannel(int channel) {
        OpensyncAPVIFState ret = null;

        for (OpensyncAPVIFState vif : vifStates) {
            if (vif.getChannel() == channel) {
                ret = vif;
                break;
            }
        }

        return ret;
    }

    public List<OpensyncAPVIFState> getVIFsForSSID(String ssid) {
        List<OpensyncAPVIFState> ret = new ArrayList<OpensyncAPVIFState>();

        for (OpensyncAPVIFState vif : vifStates) {
            if (vif.getSsid().equals(ssid)) {
                ret.add(vif);
            }
        }

        return ret;
    }

    public OpensyncAPInetState getInetForMac(MacAddress mac) {
        OpensyncAPInetState ret = null;
        for (OpensyncAPInetState inet : inetStates) {
            if (inet.getHwAddr().equals(mac.toString())) {
                ret = inet;
                break;
            }
        }

        return ret;
    }

    public boolean deleteWifiClient(String deletedClientMacAddress) {
        for (OpensyncWifiAssociatedClients client : wifiClients) {
            if (client.getMac().equals(deletedClientMacAddress)) {
                wifiClients.remove(client);
                return true;
            }
        }
        // could not find this client
        return false;
    }

    public boolean deleteVif(OpensyncAPVIFState toBeDeleted) {
        for (OpensyncAPVIFState vif : vifStates) {
            if (vif.getSsid().equals(toBeDeleted.getSsid()) && vif.getIfName().equals(toBeDeleted.getIfName())) {
                vifStates.remove(vif);
                return true;
            }
        }
        // could not find this vif
        return false;
    }

}
