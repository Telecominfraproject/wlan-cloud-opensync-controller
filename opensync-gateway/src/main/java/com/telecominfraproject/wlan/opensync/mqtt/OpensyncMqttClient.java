package com.telecominfraproject.wlan.opensync.mqtt;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fusesource.mqtt.client.FutureConnection;
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
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import com.telecominfraproject.wlan.opensync.external.integration.OpensyncExternalIntegrationInterface;
import com.telecominfraproject.wlan.opensync.util.ZlibUtil;

import sts.OpensyncStats;
import sts.OpensyncStats.Report;
import traffic.NetworkMetadata;
import traffic.NetworkMetadata.FlowReport;
import wc.stats.IpDnsTelemetry;
import wc.stats.IpDnsTelemetry.WCStatsReport;

@Profile("mqtt_receiver")
@Component
public class OpensyncMqttClient implements ApplicationListener<ContextClosedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensyncMqttClient.class);

    private static final Logger MQTT_LOG = LoggerFactory.getLogger("MQTT_DATA");

    public static Charset utf8 = Charset.forName("UTF-8");

    @Autowired
    private OpensyncExternalIntegrationInterface extIntegrationInterface;

    //
    // See https://github.com/fusesource/mqtt-client for the docs
    //

    private boolean keepReconnecting = true;
    private Thread mqttClientThread;

    public OpensyncMqttClient(@Autowired io.netty.handler.ssl.SslContext sslContext,
            @Value("${tip.wlan.mqttBroker.address.internal:testportal.123wlan.com}") String mqttBrokerAddress,
            @Value("${tip.wlan.mqttBroker.listenPort:1883}") int mqttBrokerListenPort,
            @Value("${tip.wlan.mqttBroker.user:admin}") String username,
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
                    FutureConnection futureConnection = null;
                    try {
                        Thread.sleep(5000);

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
                        // mqtt.setHost("tcp://192.168.0.137:61616");
                        mqtt.setHost("tls://" + mqttBrokerAddress + ":" + mqttBrokerListenPort);
                        LOG.info("Connecting to MQTT broker at {}", mqtt.getHost());
                        mqtt.setClientId("opensync_mqtt");
                        mqtt.setUserName(username);
                        mqtt.setPassword(password);
                        // Note: the following does not work with the
                        // serverContext,
                        // it has to be the
                        // clientContext
                        // mqtt.setSslContext(((JdkSslContext)
                        // sslContext).context());
                        // For now we'll rely on regular SSLContext from the JDK

                        // TODO: revisit this blocking connection, change it to
                        // futureConnection
                        futureConnection = mqtt.futureConnection();
                        futureConnection.connect();

                        LOG.info("Connected to MQTT broker at {}", mqtt.getHost());

                        // Subscribe to topics:
                        //
                        // new Topic("mqtt/example/publish", QoS.AT_LEAST_ONCE),
                        // new Topic("#", QoS.AT_LEAST_ONCE),
                        // new Topic("test/#", QoS.EXACTLY_ONCE),
                        // new Topic("foo/+/bar", QoS.AT_LEAST_ONCE)
                        Topic[] topics = { new Topic("#", QoS.AT_LEAST_ONCE), };

                        futureConnection.subscribe(topics);
                        LOG.info("Subscribed to mqtt topics {}", Arrays.asList(topics));

                        // prepare a JSONPrinter to format protobuf messages as
                        // json
                        List<Descriptors.Descriptor> protobufDescriptors = new ArrayList<>();
                        protobufDescriptors.addAll(OpensyncStats.getDescriptor().getMessageTypes());
                        protobufDescriptors.addAll(IpDnsTelemetry.getDescriptor().getMessageTypes());
                        protobufDescriptors.addAll(NetworkMetadata.getDescriptor().getMessageTypes());
                        TypeRegistry oldRegistry = TypeRegistry.newBuilder().add(protobufDescriptors).build();
                        JsonFormat.Printer jsonPrinter = JsonFormat.printer().includingDefaultValueFields()
                                .omittingInsignificantWhitespace().usingTypeRegistry(oldRegistry);

                        // main loop - receive messages
                        while (true) {
                            Message mqttMsg = futureConnection.receive().await();

                            if (mqttMsg == null) {
                                continue;
                            }

                            LOG.debug("MQTT Topic {}", mqttMsg.getTopic());

                            byte payload[] = mqttMsg.getPayload();
                            // we acknowledge right after receive because:
                            // a. none of the stats messages are so important
                            // that
                            // we cannot skip one
                            // b. if there's some kind of problem with the
                            // message
                            // (decoding or processing)
                            // - we want to move on as quickly as possible and
                            // not
                            // let it get stuck in the
                            // queue
                            mqttMsg.ack();

                            LOG.trace("received message on topic {} size {}", mqttMsg.getTopic(), payload.length);

                            if (payload[0] == 0x78) {
                                // looks like zlib-compressed data, let's
                                // decompress
                                // it before deserializing
                                payload = ZlibUtil.decompress(payload);
                            }

                            // attempt to parse the message as protobuf
                            MessageOrBuilder encodedMsg = null;
                            try {

                                encodedMsg = Report.parseFrom(payload);

                                MQTT_LOG.info("topic = {} Report = {}", mqttMsg.getTopic(),
                                        jsonPrinter.print(encodedMsg));


                                extIntegrationInterface.processMqttMessage(mqttMsg.getTopic(), (Report) encodedMsg);

                            } catch (Exception e) {
                                try {
                                    // not a opensync_stats report, attempt to
                                    // deserialize as network_metadata
                                    encodedMsg = FlowReport.parseFrom(payload);

                                    MQTT_LOG.info("topic = {} FlowReport = {}", mqttMsg.getTopic(),
                                            jsonPrinter.print(encodedMsg));

                                    extIntegrationInterface.processMqttMessage(mqttMsg.getTopic(),
                                            (FlowReport) encodedMsg);
                                } catch (Exception e1) {

                                    try {
                                        // not a opensync_stats report and not
                                        // network_metadata report, attempt to
                                        // deserialize as WCStatsReport
                                        encodedMsg = WCStatsReport.parseFrom(payload);

                                        MQTT_LOG.info("topic = {} IpDnsTelemetry = {}", mqttMsg.getTopic(),
                                                jsonPrinter.print(encodedMsg));


                                        extIntegrationInterface.processMqttMessage(mqttMsg.getTopic(),
                                                (WCStatsReport) encodedMsg);
                                    } catch (Exception e2) {
                                        String msgStr = new String(mqttMsg.getPayload(), utf8);
                                        MQTT_LOG.info("topic = {} message = {}", mqttMsg.getTopic(), msgStr);
                                    }
                                }
                            }

                        }

                    } catch (Exception e) {
                        LOG.error("Exception in MQTT receiver", e);
                    } finally {
                        try {
                            if (futureConnection != null) {
                                futureConnection.disconnect();
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
