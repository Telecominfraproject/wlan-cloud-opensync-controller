package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;
import com.vmware.ovsdb.service.impl.OvsdbPassiveConnectionListenerImpl;

@Configuration
public class OvsdbListenerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbListenerConfig.class);

    @Bean
    public OvsdbPassiveConnectionListener ovsdbPassiveConnectionListener(
            @org.springframework.beans.factory.annotation.Value("${connectus.ovsdb.listener.threadPoolSize:10}")
            int threadPoolSize) {
        LOG.debug("Configuring OvsdbPassiveConnectionListener with thread pool size {}", threadPoolSize);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(threadPoolSize);    
        OvsdbPassiveConnectionListener listener = new OvsdbPassiveConnectionListenerImpl(executorService);
        return listener;
    }    
}
