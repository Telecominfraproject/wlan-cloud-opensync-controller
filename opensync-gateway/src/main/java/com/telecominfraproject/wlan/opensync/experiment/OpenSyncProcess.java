package com.telecominfraproject.wlan.opensync.experiment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages={"com.telecominfraproject.wlan"})
@EnableAutoConfiguration
public class OpenSyncProcess {

    /**
     * <br>{@code java -Dssl.props=file:./ssl.properties -Dlogback.configurationFile=file:./logback.xml -jar ./opensync-experiment-0.0.1-SNAPSHOT.jar}
     *  
     * @param args
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(OpenSyncProcess.class, args);
        // signal start of the application context
        applicationContext.start();
    }
}
