package com.telecominfraproject.wlan.opensync.util;

import java.lang.reflect.Field;

import com.telecominfraproject.wlan.opensync.ovsdb.metrics.OvsdbClientWithMetrics;
import com.vmware.ovsdb.service.OvsdbClient;
import com.vmware.ovsdb.service.impl.OvsdbClientImpl;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * Uses reflection to associate routingId with low-level netty channel.
 * 
 * @author dtop
 * 
 */
public class OvsdbClientUtil {
    
    public static final AttributeKey<String> routingRecordIdAttrKey = AttributeKey.valueOf("routingRecordId");


    public static void setRoutingId(OvsdbClient ovsdbClient, long routingId) {
        Channel channel = getChannel(ovsdbClient);
        channel.attr(routingRecordIdAttrKey).set(Long.toString(routingId));
    }

    public static Long getRoutingId(OvsdbClient ovsdbClient) {
        Channel channel = getChannel(ovsdbClient);
        String strVal = channel.attr(routingRecordIdAttrKey).get();
        return strVal==null?null:Long.parseLong(strVal);        
    }

    
    private static Channel getChannel(OvsdbClient ovsdbClient) {
        if(ovsdbClient instanceof OvsdbClientWithMetrics) {
            //unwrap the object, if needed
            ovsdbClient = ((OvsdbClientWithMetrics) ovsdbClient).getDelegate();
        }
        
        if(! (ovsdbClient instanceof OvsdbClientImpl)) {
            throw new RuntimeException("Do not know how to handle "+ ovsdbClient.getClass().getName()+" - expected OvsdbClientImpl");
        }
        
        try {
            Field jsonRpcClientField = ovsdbClient.getClass().getDeclaredField("jsonRpcClient");
            jsonRpcClientField.setAccessible(true);
            Object jsonRpcClientObj = jsonRpcClientField.get(ovsdbClient);
            
            Field transporterField = jsonRpcClientObj.getClass().getDeclaredField("transporter");
            transporterField.setAccessible(true);
            Object transporterObj = transporterField.get(jsonRpcClientObj);
            
            Field channelField = transporterObj.getClass().getDeclaredField("val$channel");
            channelField.setAccessible(true);
            
            Channel channel = (Channel) channelField.get(transporterObj);
            
            return channel;
        }catch(Exception e) {
            throw new RuntimeException("Cannot get the channel for the ovsdbClient "+ ovsdbClient, e);            
        }
    }
    
}
