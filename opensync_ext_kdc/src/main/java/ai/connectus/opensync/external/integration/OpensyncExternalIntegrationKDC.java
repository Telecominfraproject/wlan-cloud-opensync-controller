package ai.connectus.opensync.external.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.whizcontrol.core.model.equipment.DetectedAuthMode;
import com.whizcontrol.core.model.equipment.EquipmentType;
import com.whizcontrol.core.model.equipment.MacAddress;
import com.whizcontrol.core.model.equipment.NeighboreScanPacketType;
import com.whizcontrol.core.model.equipment.NetworkType;
import com.whizcontrol.core.model.equipment.RadioType;
import com.whizcontrol.core.model.equipment.Toggle;
import com.whizcontrol.equipmentandnetworkconfig.models.ApElementConfiguration;
import com.whizcontrol.equipmentandnetworkconfig.models.ApElementConfiguration.ApModel;
import com.whizcontrol.equipmentandnetworkconfig.models.CountryCode;
import com.whizcontrol.equipmentandnetworkconfig.models.DeviceMode;
import com.whizcontrol.equipmentandnetworkconfig.models.ElementRadioConfiguration;
import com.whizcontrol.equipmentandnetworkconfig.models.EquipmentElementConfiguration;
import com.whizcontrol.equipmentandnetworkconfig.models.GettingDNS;
import com.whizcontrol.equipmentandnetworkconfig.models.GettingIP;
import com.whizcontrol.equipmentandnetworkconfig.models.SsidConfiguration;
import com.whizcontrol.equipmentandnetworkconfig.models.SsidConfiguration.AppliedRadio;
import com.whizcontrol.equipmentandnetworkconfig.models.SsidConfiguration.SecureMode;
import com.whizcontrol.equipmentandnetworkconfig.models.StateSetting;
import com.whizcontrol.equipmentandnetworkmanagement.EquipmentAndNetworkManagementInterface;
import com.whizcontrol.equipmentconfigurationmanager.EquipmentConfigurationManagerInterface;
import com.whizcontrol.equipmentconfigurationmanager.models.ResolvedEquipmentConfiguration;
import com.whizcontrol.equipmentinventory.models.CustomerEquipment;
import com.whizcontrol.equipmentmetricscollector.EquipmentMetricsCollectorInterface;
import com.whizcontrol.equipmentrouting.EquipmentRoutingInterface;
import com.whizcontrol.equipmentroutinginfo.models.EquipmentRoutingRecord;
import com.whizcontrol.orderandsubscriptionmanagement.OrderAndSubscriptionManagementInterface;
import com.whizcontrol.servicemetrics.models.APDemoMetric;
import com.whizcontrol.servicemetrics.models.ApClientMetrics;
import com.whizcontrol.servicemetrics.models.ApPerformance;
import com.whizcontrol.servicemetrics.models.ClientMetrics;
import com.whizcontrol.servicemetrics.models.EthernetLinkState;
import com.whizcontrol.servicemetrics.models.NeighbourReport;
import com.whizcontrol.servicemetrics.models.NeighbourScanReports;
import com.whizcontrol.servicemetrics.models.SingleMetricRecord;

import ai.connectus.opensync.external.integration.controller.OpensyncKDCGatewayController;
import ai.connectus.opensync.external.integration.models.OpensyncAPConfig;
import ai.connectus.opensync.external.integration.models.OpensyncAPRadioConfig;
import ai.connectus.opensync.external.integration.models.OpensyncAPSsidConfig;
import sts.PlumeStats.Client;
import sts.PlumeStats.ClientReport;
import sts.PlumeStats.Device;
import sts.PlumeStats.Device.RadioTemp;
import sts.PlumeStats.Neighbor;
import sts.PlumeStats.Neighbor.NeighborBss;
import sts.PlumeStats.RadioBandType;
import sts.PlumeStats.Report;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@Profile("opensync_kdc_config")
@Component
public class OpensyncExternalIntegrationKDC implements OpensyncExternalIntegrationInterface {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncExternalIntegrationKDC.class);
    
    @Autowired
    private EquipmentAndNetworkManagementInterface eqNetworkManagementInterface;

    @Autowired
    private EquipmentConfigurationManagerInterface eqConfigurationManagerInterface;

    @Autowired
    private OrderAndSubscriptionManagementInterface orderAndSubscriptionManagementInterface;

    @Autowired
    private EquipmentMetricsCollectorInterface equipmentMetricsCollectorInterface;
    
    /**
     * Equipment routing provide the qrCode to CE gateway mapping
     */
    @Autowired
    private EquipmentRoutingInterface eqRoutingInterface;

    @Autowired
    private OpensyncKDCGatewayController kdcGwController;

    @Autowired
    private OvsdbSessionMapInterface ovsdbSessionMapInterface;

    @Autowired
    private CacheManager cacheManagerShortLived;
    
    @Value("${connectus.ovsdb.autoProvisionedCustomerId:1004}")
    private int autoProvisionedCustomerId;
    @Value("${connectus.ovsdb.autoProvisionedLocationId:2}")
    private int autoProvisionedLocationId;
    @Value("${connectus.ovsdb.autoProvisionedNetworkConfigId:1}")
    private long autoProvisionedNetworkConfigId;

    private Cache kdcEquipmentRecordCache;
    
    @PostConstruct
    private void postCreate(){
        LOG.info("Using KDC integration");
        kdcEquipmentRecordCache = cacheManagerShortLived.getCache("KDC_equipment_record_cache");        
    }
    
    public void apConnected(String apId) {
        LOG.info("AP {} got connected to the gateway", apId);
        try {
            
            CustomerEquipment ce = null;
            
            try {
                ce = kdcEquipmentRecordCache.get(apId, new Callable<CustomerEquipment>() {
                    @Override
                    public CustomerEquipment call() throws Exception {
                        return eqNetworkManagementInterface.getCustomerEquipmentByQrCode(apId);
                    }
                });
            }catch (Exception e) {
                //do nothing
            }

            if(ce == null) {
                
                //auto-provision APs for the demo
                //we'll use hardcoded customerId/locationId/networkConfigId for the new equipment
                
                ce = new CustomerEquipment();
                ce.setEquipmentType(EquipmentType.AP);
                ce.setModelId("plume");
                ce.setQrCode(apId);
                ce = eqNetworkManagementInterface.createCustomerEquipment(ce);
                
                //bind newly created CE to specified customer
                Set<Long> equipmentIdsToAdd = new HashSet<Long>();
                equipmentIdsToAdd.add(ce.getId());
                orderAndSubscriptionManagementInterface.bindEquipment(autoProvisionedCustomerId, equipmentIdsToAdd);

                // now update CE record itself
                ce.setName(apId);
                ce.setCustomerId(autoProvisionedCustomerId);
                ce.setEquipmentNetworkConfigId(autoProvisionedNetworkConfigId);
                ce.setLocation(autoProvisionedLocationId);

                ce = eqNetworkManagementInterface.updateCustomerEquipment(ce);

                // create the element configuration
                LOG.debug("Creating element configuration for AP({})", apId);

                EquipmentElementConfiguration equipmentElementConfiguration = new EquipmentElementConfiguration();
                EquipmentType equipmentType = ce.getEquipmentType();

                equipmentElementConfiguration.setEquipmentId(ce.getId());
                equipmentElementConfiguration.setEquipmentType(equipmentType);
                equipmentElementConfiguration.setCustomerId(ce.getCustomerId());
                equipmentElementConfiguration.setElementConfigVersion("" + equipmentType + "-V1");

                ApElementConfiguration apElementConfiguration = ApElementConfiguration.createWithDefaults(
                        equipmentElementConfiguration.getElementConfigVersion(),
                        ApModel.INDOOR);

                apElementConfiguration.setGettingIP(GettingIP.dhcp);
                apElementConfiguration.setGettingDNS(GettingDNS.dhcp);
                apElementConfiguration.setDeviceMode(DeviceMode.standaloneAP);

                equipmentElementConfiguration.setApElementConfig(apElementConfiguration);

                equipmentElementConfiguration = eqNetworkManagementInterface
                        .createEquipmentElementConfiguration(equipmentElementConfiguration);
                
                //cache newly created AP
                kdcEquipmentRecordCache.put(apId, ce);
            }
            

            EquipmentRoutingRecord equipmentRoutingRecord = new EquipmentRoutingRecord();
            equipmentRoutingRecord.setGatewayRecordId(kdcGwController.getRegisteredGwId());
            equipmentRoutingRecord.setCustomerId(ce.getCustomerId());
            equipmentRoutingRecord.setEquipmentId(ce.getId());
            EquipmentRoutingRecord ret = eqRoutingInterface.registerUERoute(equipmentRoutingRecord);
            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            ovsdbSession.setRoutingId(ret.getId());
            ovsdbSession.setEquipmentId(ce.getId());
            ovsdbSession.setCustomerId(ce.getCustomerId());
            
        }catch(Exception e) {
            LOG.error("Exception when registering ap routing {}", apId, e);
        }
    }

    public void apDisconnected(String apId) {
        LOG.info("AP {} got disconnected from the gateway", apId);
        try {
            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

            if(ovsdbSession!=null) {
                eqRoutingInterface.deregisterUserEquipment(ovsdbSession.getEquipmentId());
            } else {
                LOG.warn("Cannot find ap {} in KDC inventory", apId);
            }
        }catch(Exception e) {
            LOG.error("Exception when registering ap routing {}", apId, e);
        }
        
    }

    public OpensyncAPConfig getApConfig(String apId) {
        LOG.info("Retrieving config for AP {} ", apId);
        OpensyncAPConfig ret = null;
        
        try {
            
            OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);
            if(ovsdbSession == null) {
                throw new IllegalStateException("AP is not connected " + apId);
            }
            long equipmentId = ovsdbSession.getEquipmentId();            
            ResolvedEquipmentConfiguration resolvedEqCfg = eqConfigurationManagerInterface.getResolvedEquipmentConfiguration(equipmentId, null, null);
            
            if(resolvedEqCfg==null) {
                throw new IllegalStateException("Cannot retrieve configuration for " + apId);
            }
            ret = new OpensyncAPConfig();

            //extract country, radio channels from resolvedEqCfg
            String country = "CA";
            CountryCode countryCode = resolvedEqCfg.getEquipmentCountryCode();
            if(countryCode!=null && countryCode!=CountryCode.UNSUPPORTED) {
                country = countryCode.toString().toUpperCase();
            }
            
            int radioChannel24G = 1;
            int radioChannel5LG = 44;
            
            Map<Integer, ElementRadioConfiguration> erc = resolvedEqCfg.getEquipmentElementConfiguration().getApElementConfig().getRadioMap();
            if(erc!=null) {
                ElementRadioConfiguration erc24 = erc.get(1);
                ElementRadioConfiguration erc5 = erc.get(0);
                
                if(erc24!=null) {
                    radioChannel24G = erc24.getChannelNumber();
                }
                
                if(erc5!=null) {
                    radioChannel5LG = erc5.getChannelNumber();
                }
            }
            
            OpensyncAPRadioConfig radioConfig = new OpensyncAPRadioConfig();
            radioConfig.setCountry(country);
            radioConfig.setRadioChannel24G(radioChannel24G);
            radioConfig.setRadioChannel5LG(radioChannel5LG);
            //hardcoding this one, as there are no config for it in KDC
            radioConfig.setRadioChannel5HG(108);

            ret.setRadioConfig(radioConfig);
            
            //extract ssid parameters from resolvedEqCfg
            List<OpensyncAPSsidConfig> ssidConfigs = new ArrayList<>();            
            ret.setSsidConfigs(ssidConfigs);
            
            List<SsidConfiguration> resolvedSsids = resolvedEqCfg.getSsidConfigurations();
            if(resolvedSsids!=null) {
                for(SsidConfiguration ssidCfg : resolvedSsids) {
                    OpensyncAPSsidConfig osSsidCfg = new OpensyncAPSsidConfig();
                    osSsidCfg.setSsid(ssidCfg.getSsid());
                    
                    AppliedRadio ar = ssidCfg.getAppliedRadio();
                    if(ar == AppliedRadio.radioA) {
                        osSsidCfg.setRadioType(RadioType.is5GHz);
                    } else if (ar == AppliedRadio.radioB ){
                        osSsidCfg.setRadioType(RadioType.is2dot4GHz);
                    } else if (ar == AppliedRadio.radioAandB ){
                        osSsidCfg.setRadioType(RadioType.is5GHz);
                    }
                    
                    osSsidCfg.setBroadcast(ssidCfg.getBroadcastSsid() == StateSetting.enabled);
                    
                    if(ssidCfg.getSecureMode() == SecureMode.wpa2OnlyPSK || ssidCfg.getSecureMode() == SecureMode.wpa2PSK) {
                        osSsidCfg.setEncryption("WPA-PSK");
                        osSsidCfg.setMode("2");
                    } else if(ssidCfg.getSecureMode() == SecureMode.wpaPSK ) {
                        osSsidCfg.setEncryption("WPA-PSK");
                        osSsidCfg.setMode("1");
                    } else {
                        LOG.warn("Unsupported encryption mode {} - will use WPA-PSK instead", ssidCfg.getSecureMode());
                        osSsidCfg.setEncryption("WPA-PSK");
                        osSsidCfg.setMode("2");
                    }

                    osSsidCfg.setKey(ssidCfg.getKeyStr());

                    ssidConfigs.add(osSsidCfg);
                    
                    if( ar==AppliedRadio.radioAandB ) {
                        //configure the same ssid on the second radio
                        osSsidCfg = osSsidCfg.clone();
                        osSsidCfg.setRadioType(RadioType.is2dot4GHz);
                        ssidConfigs.add(osSsidCfg);
                    }
                }
            }
            
        } catch (Exception e) {
            LOG.error("Cannot read config for AP {}", apId, e);
        }

        LOG.debug("Config content : {}", ret);
        
        return ret;
    }


    /**
     * @param topic
     * @return apId extracted from the topic name, or null if it cannot be extracted
     */
    public String extractApIdFromTopic(String topic) {
        //Topic is formatted as "/ap/"+clientCn+"_"+ret.serialNumber+"/opensync"
        if(topic==null) {
            return null;
        }
        
        String[] parts = topic.split("/");
        if(parts.length<3) {
            return null;
        }
        
        //apId is the second element in the topic
        return parts[1];
    }

    /**
     * @param topic
     * @return customerId looked up from the topic name, or -1 if it cannot be extracted
     */
    public int extractCustomerIdFromTopic(String topic) {
        
        String apId = extractApIdFromTopic(topic);
        if(apId == null) {
            return -1;
        }
        
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if(ovsdbSession!=null) {
            return ovsdbSession.getCustomerId();
        }
        
        return -1;

    }

    public long extractEquipmentIdFromTopic(String topic) {
        
        String apId = extractApIdFromTopic(topic);
        if(apId == null) {
            return -1;
        }
        
        OvsdbSession ovsdbSession = ovsdbSessionMapInterface.getSession(apId);

        if(ovsdbSession!=null) {
            return ovsdbSession.getEquipmentId();
        }
        
        return -1;

    }

    public void processMqttMessage(String topic, Report report) {
        LOG.info("Received report on topic {} for ap {}", topic, report.getNodeID());
        int customerId = extractCustomerIdFromTopic(topic);
        if(customerId>0) {
            kdcGwController.updateActiveCustomer(customerId);
        }
        
        long equipmentId = extractEquipmentIdFromTopic(topic);
        if(equipmentId <= 0 || customerId <=0) {
            LOG.warn("Cannot determine equipment ids from topic {} - customerId {} equipmentId {}", topic, customerId, equipmentId);
            return;
        }
        
        List<SingleMetricRecord> metricRecordList = new ArrayList<>();
        
        populateAPDemoMetrics(metricRecordList, report, customerId, equipmentId);
        populateApClientMetrics(metricRecordList, report, customerId, equipmentId);
        populateNeighbourScanReports(metricRecordList, report, customerId, equipmentId);

        populateApSsidMetrics(metricRecordList, report, customerId, equipmentId);
        populateChannelInfoReports(metricRecordList, report, customerId, equipmentId);
        
        if(!metricRecordList.isEmpty()) {
            equipmentMetricsCollectorInterface.createRecordList(metricRecordList);
        }

    }

    private void populateNeighbourScanReports(List<SingleMetricRecord> metricRecordList, Report report, int customerId, long equipmentId) {

        for(Neighbor neighbor: report.getNeighborsList()){
            
            SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
            metricRecordList.add(smr);
            NeighbourScanReports neighbourScanReports = new NeighbourScanReports();
            smr.setData(neighbourScanReports);
            
            smr.setCreatedTimestamp(neighbor.getTimestampMs());
            
            List<NeighbourReport> neighbourReports = new ArrayList<>();
            neighbourScanReports.setNeighbourReports(neighbourReports);
            
            for(NeighborBss nBss: neighbor.getBssListList()) {
                NeighbourReport nr = new NeighbourReport();
                neighbourReports.add(nr);
                
    //            "band": "BAND5GL",
    //            "scanType": "ONCHAN_SCAN",
    //            "timestampMs": "1581118421629",
    //            "bssList": [
    //              {
    //                "bssid": "80:D0:4A:E6:66:C9",
    //                "ssid": "",
    //                "rssi": 4,
    //                "tsf": "0",
    //                "chanWidth": "CHAN_WIDTH_80MHZ",
    //                "channel": 44,
    //                "status": "ADDED"
    //              },

                if(neighbor.getBand()==RadioBandType.BAND2G) {
                    nr.setAcMode(Toggle.off);
                    nr.setbMode(Toggle.off);
                    nr.setnMode(Toggle.on);
                    nr.setRadioType(RadioType.is2dot4GHz);
                } else {
                    nr.setAcMode(Toggle.on);
                    nr.setbMode(Toggle.off);
                    nr.setnMode(Toggle.off);
                    nr.setRadioType(RadioType.is5GHz);                    
                }

                nr.setChannel(nBss.getChannel());
                nr.setMacAddress(MacAddress.valueOf(nBss.getBssid()).getAddress());
                nr.setNetworkType(NetworkType.AP);
                nr.setPacketType(NeighboreScanPacketType.BEACON);
                nr.setPrivacy((nBss.getSsid()==null || nBss.getSsid().isEmpty())?Toggle.on:Toggle.off);
                //nr.setRate(rate);
                nr.setRssi(nBss.getRssi());
                //nr.setScanTimeInSeconds(scanTimeInSeconds);
                nr.setSecureMode(DetectedAuthMode.WPA);
                //nr.setSignal(signal);
                nr.setSsid(nBss.getSsid());
            }
        }  
    }

    private void populateChannelInfoReports(List<SingleMetricRecord> metricRecordList, Report report, int customerId, long equipmentId) {

        //TODO: implement me!
        //TODO: continue from here --->>>


//      {
//      SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
//      metricRecordList.add(smr);
//      ChannelInfoReports channelInfoReports = new ChannelInfoReports();
//      smr.setData(channelInfoReports);
//      
//      List<ChannelInfo> channelInformationReports2g = new ArrayList<>();
//      channelInfoReports.setChannelInformationReports2g(channelInformationReports2g);
//
//      List<ChannelInfo> channelInformationReports5g = new ArrayList<>();
//      channelInfoReports.setChannelInformationReports5g(channelInformationReports5g);
//      
//      ChannelInfo chInfo = new ChannelInfo();
//      chInfo.setBandwidth(bandwidth);
//      chInfo.setChanNumber(chanNumber);
//      chInfo.setNoiseFloor(noiseFloor);
//      chInfo.setTotalUtilization(totalUtilization);
//      chInfo.setWifiUtilization(wifiUtilization);
//      
//      channelInformationReports2g.add(chInfo);
//      
//      chInfo = new ChannelInfo();
//      chInfo.setBandwidth(bandwidth);
//      chInfo.setChanNumber(chanNumber);
//      chInfo.setNoiseFloor(noiseFloor);
//      chInfo.setTotalUtilization(totalUtilization);
//      chInfo.setWifiUtilization(wifiUtilization);
//      
//      channelInformationReports5g.add(chInfo);
//
//  }
    }

    private void populateApSsidMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId, long equipmentId) {
        //TODO: implement me!
//      {
//      SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
//      metricRecordList.add(smr);
//
//      ApSsidMetrics apSsidMetrics = new ApSsidMetrics();
//      smr.setData(apSsidMetrics);
//      
//      List<SsidStatistics> ssidStats2g = new ArrayList<>();
//      apSsidMetrics.setSsidStats2g(ssidStats2g );
//
//      List<SsidStatistics> ssidStats5g = new ArrayList<>();
//      apSsidMetrics.setSsidStats5g(ssidStats5g );
//
//      SsidStatistics ssidStat = new SsidStatistics();        
//      ssidStats2g.add(ssidStat);
//      ssidStat.setBssid(bssid);
//      ssidStat.setSsid(ssid);
//      ssidStat.setNumClient(numClient);
//      ssidStat.setRxBytes(rxBytes);
//      ssidStat.setRxLastRssi(rxLastRssi);
//      ssidStat.setNumTxBytesSucc(numTxBytesSucc);
//      
//      ssidStat = new SsidStatistics();        
//      ssidStats5g.add(ssidStat);
//      ssidStat.setBssid(bssid);
//      ssidStat.setSsid(ssid);
//      ssidStat.setNumClient(numClient);
//      ssidStat.setRxBytes(rxBytes);
//      ssidStat.setRxLastRssi(rxLastRssi);
//      ssidStat.setNumTxBytesSucc(numTxBytesSucc);
//  }        
    }

    private void populateApClientMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId, long equipmentId) {
        
        for(ClientReport clReport: report.getClientsList()){
            SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
            metricRecordList.add(smr);
    
            ApClientMetrics apClientMetrics = new ApClientMetrics();
            smr.setData(apClientMetrics);
            smr.setCreatedTimestamp(clReport.getTimestampMs());
            
            Integer periodLengthSec = 60; //matches what's configured by OvsdbDao.configureStats(OvsdbClient)
            apClientMetrics.setPeriodLengthSec(periodLengthSec);
            
            List<ClientMetrics> clientMetrics = new ArrayList<>();
            
            for(Client cl: clReport.getClientListList()) {
                //clReport.getChannel();
                ClientMetrics cMetrics = new ClientMetrics();
                clientMetrics.add(cMetrics);
                cMetrics.setRadioType((clReport.getBand() == RadioBandType.BAND2G)?RadioType.is2dot4GHz:RadioType.is5GHz);                
                cMetrics.setDeviceMacAddress(new MacAddress(cl.getMacAddress()));
                
                if(cl.hasStats()) {
                    if(cl.getStats().hasRssi()) {
                        cMetrics.setRssi(cl.getStats().getRssi());
                    }
                    
                    //we'll report each device as having a single (very long) session
                    cMetrics.setSessionId(cMetrics.getDeviceMacAddress().getAddressAsLong());
                    
                    //populate Rx stats
                    if(cl.getStats().hasRxBytes()) {
                        cMetrics.setRxBytes(cl.getStats().getRxBytes());
                    }
                    
                    if(cl.getStats().hasRxRate()) {
                        //cMetrics.setAverageRxRate(cl.getStats().getRxRate());
                    }
                    
                    if(cl.getStats().hasRxErrors()) {
                        cMetrics.setNumRxNoFcsErr((int)cl.getStats().getRxErrors());
                    }
                    
                    if(cl.getStats().hasRxFrames()) {
                        //cMetrics.setNumRxFramesReceived(cl.getStats().getRxFrames());
                        cMetrics.setNumRxPackets(cl.getStats().getRxFrames());
                    }

                    if(cl.getStats().hasRxRetries()) {
                        cMetrics.setNumRxRetry((int)cl.getStats().getRxRetries());
                    }
                    
                    //populate Tx stats
                    if(cl.getStats().hasTxBytes()) {
                        cMetrics.setNumTxBytes(cl.getStats().getTxBytes());
                    }
                    
                    if(cl.getStats().hasTxRate()) {
                        //cMetrics.setAverageTxRate(cl.getStats().getTxRate());
                    }
                    
                    if(cl.getStats().hasTxRate() && cl.getStats().hasRxRate()) {
                        cMetrics.setRates(new byte[]{(byte) (cl.getStats().getTxRate()), (byte) (cl.getStats().getRxRate())});
                    }
                    
                    if(cl.getStats().hasTxErrors()) {
                        cMetrics.setNumTxDropped((int)cl.getStats().getTxErrors());
                    }
                    
                    if(cl.getStats().hasTxFrames()) {
                        //cMetrics.setNumTxFramesTransmitted(cl.getStats().getTxFrames());
                        cMetrics.setNumTxPackets(cl.getStats().getRxFrames());
                    }

                    if(cl.getStats().hasTxRetries()) {
                        cMetrics.setNumTxDataRetries((int)cl.getStats().getTxRetries());
                    }

                }
            }

            if(clReport.getBand() == RadioBandType.BAND2G) {
                apClientMetrics.setClientMetrics2g(clientMetrics.toArray(new ClientMetrics[0]));
            } else {
                apClientMetrics.setClientMetrics5g(clientMetrics.toArray(new ClientMetrics[0]));
            }
            
        }        
    }

    private void populateAPDemoMetrics(List<SingleMetricRecord> metricRecordList, Report report, int customerId, long equipmentId) {
        for(Device deviceReport : report.getDeviceList()) {
            
            SingleMetricRecord smr = new SingleMetricRecord(customerId, equipmentId);
            metricRecordList.add(smr);

            APDemoMetric data = new APDemoMetric();
            smr.setData(data);
            ApPerformance apPerformance = new ApPerformance();
            data.setApPerformance(apPerformance);

            smr.setCreatedTimestamp(deviceReport.getTimestampMs());
//            data.setChannelUtilization2G(channelUtilization2G);
//            data.setChannelUtilization5G(channelUtilization5G);
            
            if(deviceReport.getRadioTempCount()>0) {
                int cpuTemperature = 0;
                int numSamples = 0;
                for(RadioTemp r: deviceReport.getRadioTempList()) {
                    if(r.hasValue()) {
                        cpuTemperature += r.getValue();
                        numSamples++;
                    }
                }
                
                if(numSamples>0) {
                    apPerformance.setCpuTemperature( cpuTemperature / numSamples );
                }
            }
            
            if(deviceReport.hasCpuUtil() && deviceReport.getCpuUtil().hasCpuUtil()) {
                apPerformance.setCpuUtilized(new byte[]{(byte) (deviceReport.getCpuUtil().getCpuUtil()), (byte) (0)});
            }
            
            apPerformance.setEthLinkState(EthernetLinkState.UP1000_FULL_DUPLEX);
            
            if(deviceReport.hasMemUtil() && deviceReport.getMemUtil().hasMemTotal() && deviceReport.getMemUtil().hasMemUsed()) {
                apPerformance.setFreeMemory(deviceReport.getMemUtil().getMemTotal() - deviceReport.getMemUtil().getMemUsed());
            }
            apPerformance.setUpTime(new Long(deviceReport.getUptime()));
                        
        }        
    }

    public void processMqttMessage(String topic, FlowReport flowReport) {
        LOG.info("Received flowReport on topic {} for ap {}", topic, flowReport.getObservationPoint().getNodeId());
        int customerId = extractCustomerIdFromTopic(topic);
        if(customerId>0) {
            kdcGwController.updateActiveCustomer(customerId);
        }
        //TODO: implement me
    }

    public void processMqttMessage(String topic, WCStatsReport wcStatsReport) {
        LOG.info("Received wcStatsReport on topic {} for ap {}", topic, wcStatsReport.getObservationPoint().getNodeId());
        int customerId = extractCustomerIdFromTopic(topic);
        if(customerId>0) {
            kdcGwController.updateActiveCustomer(customerId);
        }
        //TODO: implement me
    }

}
