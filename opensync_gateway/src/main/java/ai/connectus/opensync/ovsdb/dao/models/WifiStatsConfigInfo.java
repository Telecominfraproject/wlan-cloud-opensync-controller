package ai.connectus.opensync.ovsdb.dao.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vmware.ovsdb.protocol.operation.notation.Uuid;

/**
 * radio_type + stats_type + survey_type identifies a record
 *
 */
public class WifiStatsConfigInfo implements Cloneable{
    
    public Set<Integer> channelList;
    public String radioType;
    public int reportingInterval;
    public int samplingInterval;
    public String statsType;
    public int surveyIntervalMs;
    public String surveyType;
    public Map<String, Integer> threshold;
                    
    public Uuid uuid;
    
    @Override
    public WifiStatsConfigInfo clone() {
        try {
            WifiStatsConfigInfo ret = (WifiStatsConfigInfo)super.clone();
            if(channelList!=null) {
                ret.channelList = new HashSet<>(this.channelList);
            }
            if(threshold!=null) {
                ret.threshold = new HashMap<>(this.threshold);
            }

            return ret;
        }catch(CloneNotSupportedException e) {                
            throw new IllegalStateException("Cannot clone ", e);
        }            
    }

    @Override
    public String toString() {
        return String.format(
                "WifiStatsConfigInfo [channelList=%s, radioType=%s, reportingInterval=%s, samplingInterval=%s, statsType=%s, surveyIntervalMs=%s, surveyType=%s, threshold=%s, uuid=%s]",
                channelList, radioType, reportingInterval, samplingInterval, statsType, surveyIntervalMs,
                surveyType, threshold, uuid);
    }

    
}