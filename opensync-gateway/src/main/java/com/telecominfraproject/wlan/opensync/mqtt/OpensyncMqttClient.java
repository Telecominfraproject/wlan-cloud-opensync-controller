
package com.telecominfraproject.wlan.opensync.mqtt;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import com.google.protobuf.Descriptors;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.tag.TagList;
import com.telecominfraproject.wlan.cloudmetrics.CloudMetricsTags;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.external.integration.utils.StatsPublisherInterface;
import com.telecominfraproject.wlan.opensync.util.ZlibUtil;

import sts.OpensyncStats;
import sts.OpensyncStats.Report;

@Profile("mqtt_receiver")
@Component
public class OpensyncMqttClient implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncMqttClient.class);

    private static final Logger MQTT_LOG = LoggerFactory.getLogger("MQTT_DATA");

    public static Charset utf8 = Charset.forName("UTF-8");

    private final TagList tags = CloudMetricsTags.commonTags;

    private final Counter messagesReceived = new BasicCounter(MonitorConfig.builder("osgw-mqtt-messagesReceived").withTags(tags).build());

    private final Counter messageBytesReceived = new BasicCounter(MonitorConfig.builder("osgw-mqtt-messageBytesReceived").withTags(tags).build());

    private final Timer timerMessageProcess = new BasicTimer(MonitorConfig.builder("osgw-mqtt-messageProcessTimer").withTags(tags).build());

    @Autowired
    private OpensyncExternalIntegrationInterface opensyncExternalIntegrationInterface;

    // dtop: use anonymous constructor to ensure that the following code always
    // get executed,
    // even when somebody adds another constructor in here
    {
        DefaultMonitorRegistry.getInstance().register(messagesReceived);
        DefaultMonitorRegistry.getInstance().register(messageBytesReceived);
        DefaultMonitorRegistry.getInstance().register(timerMessageProcess);
    }

    //
    // See https://github.com/fusesource/mqtt-client for the docs
    //

    private boolean keepReconnecting = true;
    private Thread mqttClientThread;

    public OpensyncMqttClient(@Autowired io.netty.handler.ssl.SslContext sslContext,
            @Value("${tip.wlan.mqttBroker.address.internal:testportal.123wlan.com}") String mqttBrokerAddress,
            @Value("${tip.wlan.mqttBroker.listenPort:1883}") int mqttBrokerListenPort, @Value("${tip.wlan.mqttBroker.user:admin}") String username,
            @Value("${tip.wlan.mqttBroker.password:admin}") String password,
            @Value("${mqtt.javax.net.ssl.keyStore:/opt/tip-wlan/certs/client_keystore.jks}") String jdkKeyStoreLocation,
            @Value("${mqtt.javax.net.ssl.keyStorePassword:mypassword}") String jdkKeyStorePassword,
            @Value("${mqtt.javax.net.ssl.trustStore:/opt/tip-wlan/certs/truststore.jks}") String jdkTrustStoreLocation,
            @Value("${mqtt.javax.net.ssl.trustStorePassword:mypassword}") String jdkTrustStorePassword) {

        if (System.getProperty("javax.net.ssl.keyStore") == null) {
            System.setProperty("javax.net.ssl.keyStore", jdkKeyStoreLocation);
        }

        if (System.getProperty("javax.net.ssl.keyStorePassword") == null) {
            System.setProperty("javax.net.ssl.keyStorePassword", jdkKeyStorePassword);
        }

        if (System.getProperty("javax.net.ssl.trustStore") == null) {
            System.setProperty("javax.net.ssl.trustStore", jdkTrustStoreLocation);
        }

        if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            System.setProperty("javax.net.ssl.trustStorePassword", jdkTrustStorePassword);
        }

        Runnable mqttClientRunnable = new Runnable() {

            @Override
            public void run() {
                while (keepReconnecting) {
                    BlockingConnection blockingConnection = null;
                    try {
                        // Thread.sleep(5000);

                        // Create a new MQTT connection to the broker.
                        /*
                         * Using SSL connections If you want to connect over
                         * SSL/TLS instead of TCP, use an "ssl://" or "tls://"
                         * URI prefix instead of "tcp://" for the host field.
                         * Supported protocol values are:
                         *
                         * ssl:// - Use the JVM default version of the SSL
                         * algorithm. sslv*:// - Use a specific SSL version
                         * where * is a version supported by your JVM. Example:
                         * sslv3 tls:// - Use the JVM default version of the TLS
                         * algorithm. tlsv*:// - Use a specific TLS version
                         * where * is a version supported by your JVM. Example:
                         * tlsv1.1 The client will use the default JVM
                         * SSLContext which is configured via JVM system
                         * properties unless you configure the MQTT instance
                         * using the setSslContext method.
                         *
                         * SSL connections perform blocking operations against
                         * internal thread pool unless you call the
                         * setBlockingExecutor method to configure that executor
                         * they will use instead.
                         *
                         */

                        MQTT mqtt = new MQTT();
                        mqtt.setHost("tls://" + mqttBrokerAddress + ":" + mqttBrokerListenPort);
                        LOG.info("Connecting to MQTT broker at {}", mqtt.getHost());
                        mqtt.setClientId("opensync_mqtt");
                        mqtt.setUserName(username);
                        mqtt.setPassword(password);
                        blockingConnection = mqtt.blockingConnection();
                        blockingConnection.connect();

                        LOG.debug("Connected to MQTT broker at {}", mqtt.getHost());

                        // NB. setting to AT_MOST_ONCE to match the APs message level
                        Topic[] topics = {new Topic("/ap/#", QoS.AT_MOST_ONCE),};

                        blockingConnection.subscribe(topics);
                        LOG.info("Subscribed to mqtt topics {}", Arrays.asList(topics));

                        // prepare a JSONPrinter to format protobuf messages as
                        // json
                        List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
                        protobufDescriptors.addAll(OpensyncStats.getDescriptor().getMessageTypes());
                        TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
                        JsonFormat.Printer jsonPrinter =
                                JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace().usingTypeRegistry(oldRegistry);

                        // main loop - receive messages
                        while (true) {
                            LOG.debug("{} awaiting mqtt message from broker",mqttClientThread.getName());
                            Message mqttMsg = blockingConnection.receive();

                            if (mqttMsg == null) {
                                if (LOG.isTraceEnabled())
                                    LOG.trace("NULL message received for blocking connection");
                                continue;
                            }
                            Stopwatch stopwatchTimerMessageProcess = timerMessageProcess.start();
                            try {
                                byte payload[] = mqttMsg.getPayload();
                                messagesReceived.increment();
                                messageBytesReceived.increment(payload.length);

                                if (payload[0] == 0x78) {
                                    // looks like zlib-compressed data, let's
                                    // decompress
                                    // it before deserializing
                                    payload = ZlibUtil.decompress(payload);
                                }
                                // Only supported protobuf on the TIP opensync APs is Report
                                Report statsReport = Report.parseFrom(payload);
                                mqttMsg.ack();
                                MQTT_LOG.info("Topic {}\n{}", mqttMsg.getTopic(), jsonPrinter.print(statsReport));
                                opensyncExternalIntegrationInterface.processMqttMessage(mqttMsg.getTopic(), statsReport);                           
                                LOG.debug("Dispatched report for topic {} to backend for processing", mqttMsg.getTopic());

                            } catch (Exception e) {
                                LOG.error("Exception processing topic for message {}",mqttMsg, e);
                            } finally {
                                stopwatchTimerMessageProcess.stop();
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("Exception in MQTT receiver", e);
                    } finally {
                        try {
                            if (blockingConnection != null) {
                                blockingConnection.disconnect();
                            }
                        } catch (Exception e1) {
                            // do nothing
                        }
                    }
                }

            }
        };

        mqttClientThread = new Thread(mqttClientRunnable, "mqttClientThread");
        mqttClientThread.setDaemon(true);
        mqttClientThread.start();

    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.debug("Processing ContextClosedEvent event");
        keepReconnecting = false;

        if (mqttClientThread != null) {
            mqttClientThread.interrupt();
        }
    }

}
