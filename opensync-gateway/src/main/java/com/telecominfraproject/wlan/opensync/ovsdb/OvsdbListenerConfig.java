package com.telecominfraproject.wlan.opensync.ovsdb;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.NumberGauge;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.vmware.ovsdb.service.OvsdbPassiveConnectionListener;
import com.vmware.ovsdb.service.impl.OvsdbPassiveConnectionListenerImpl;

@Configuration
public class OvsdbListenerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbListenerConfig.class);

    private final TagList tags = CloudMetricsTags.commonTags;

    final Counter rejectedTasks = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-rejectedTasks").withTags(tags).build());
    final Counter submittedTasks = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-submittedTasks").withTags(tags).build());
    final Counter completedTasks = new BasicCounter(MonitorConfig.builder("osgw-ovsdb-completedTasks").withTags(tags).build());
    
    private AtomicInteger tasksInFlight = new AtomicInteger(0);
    
    private final NumberGauge tasksInFlightGauge = new NumberGauge(
            MonitorConfig.builder("osgw-ovsdb-tasksInFlight").withTags(tags).build(), tasksInFlight);

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(rejectedTasks);
        DefaultMonitorRegistry.getInstance().register(submittedTasks);
        DefaultMonitorRegistry.getInstance().register(completedTasks);
        DefaultMonitorRegistry.getInstance().register(tasksInFlightGauge);
    }

    @Bean
    public OvsdbPassiveConnectionListener ovsdbPassiveConnectionListener(
            @org.springframework.beans.factory.annotation.Value("${tip.wlan.ovsdb.listener.threadPoolSize:10}")
            int threadPoolSize) {
        LOG.debug("Configuring OvsdbPassiveConnectionListener with thread pool size {}", threadPoolSize);
        
        ThreadFactory threadFactory = new ThreadFactory() {
            private AtomicInteger thrNum = new AtomicInteger();
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thr = new Thread(r, "ovsdb-exec-pool-" + thrNum.incrementAndGet());
                thr.setDaemon(true);
                return thr;
            }
        };
        
        RejectedExecutionHandler rejectedExecHandler = new ThreadPoolExecutor.AbortPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                rejectedTasks.increment();
                super.rejectedExecution(r, executor);
            }
        };
        
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(threadPoolSize, threadFactory, rejectedExecHandler) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                submittedTasks.increment();
                tasksInFlight.incrementAndGet();
                super.beforeExecute(t, r);
            }
            
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                completedTasks.increment();
                tasksInFlight.decrementAndGet();
                super.afterExecute(r, t);
            }
        };
        
        OvsdbPassiveConnectionListener listener = new OvsdbPassiveConnectionListenerImpl(executorService);
        return listener;
    }    
}
