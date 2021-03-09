package com.telecominfraproject.wlan.opensync.ovsdb.metrics;

import org.springframework.stereotype.Component;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;

@Component
public class OvsdbMetrics {
    
    private final TagList tags = CloudMetricsTags.commonTags;

    final Counter listDatabases = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-listDatabases").withTags(tags).build());
    final Counter getSchema = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-getSchema").withTags(tags).build());
    final Counter transact = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-transact").withTags(tags).build());
    final Counter monitor = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-monitor").withTags(tags).build());
    final Counter cancelMonitor = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-cancelMonitor").withTags(tags).build());
    final Counter lock = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-lock").withTags(tags).build());
    final Counter steal = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-steal").withTags(tags).build());
    final Counter unlock = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-unlock").withTags(tags).build());

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(listDatabases);
        DefaultMonitorRegistry.getInstance().register(getSchema);
        DefaultMonitorRegistry.getInstance().register(transact);
        DefaultMonitorRegistry.getInstance().register(monitor);
        DefaultMonitorRegistry.getInstance().register(cancelMonitor);
        DefaultMonitorRegistry.getInstance().register(lock);
        DefaultMonitorRegistry.getInstance().register(steal);
        DefaultMonitorRegistry.getInstance().register(unlock);
    }

}
