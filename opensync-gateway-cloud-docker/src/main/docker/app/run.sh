#!/bin/bash
PROFILES=" -Dspring.profiles.include=mqtt_receiver,ovsdb_redirector,ovsdb_manager"

SSL_PROPS=" "
SSL_PROPS+=" -Dssl.props=file:/opt/tip-wlan/certs/ssl.properties"
SSL_PROPS+=" -Dtip.wlan.httpClientConfig=file:/opt/tip-wlan/certs/httpClientConfig.json"

CLIENT_MQTT_SSL_PROPS=" "
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStore=/opt/tip-wlan/certs/client_keystore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStorePassword=mypassword"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStore=/opt/tip-wlan/certs/truststore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStorePassword=mypassword"
CLIENT_MQTT_SSL_PROPS+=" -Dconnectus.mqttBroker.password=admin"

OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dconnectus.ovsdb.managerAddr=opensync-controller"
OVSDB_PROPS+=" -Dconnectus.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dconnectus.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dconnectus.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dconnectus.ovsdb.trustStore=/opt/tip-wlan/certs/truststore.jks"
OVSDB_PROPS+=" -Dconnectus.ovsdb.keyStore=/opt/tip-wlan/certs/server.pkcs12"
OVSDB_PROPS+=" -Dconnectus.ovsdb.configFileName=/app/opensync/config_2_ssids.json"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dconnectus.mqttBroker.address=tip-wlan-opensync-mqtt-broker"
MQTT_PROPS+=" -Dconnectus.mqttBroker.listenPort=1883"

LOGGING_PROPS=" -Dlogging.config=file:/app/opensync/logback.xml"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443"

SPRING_EXTRA_PROPS=" --add-opens java.base/java.lang=ALL-UNNAMED"

export ALL_PROPS="$PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS $SPRING_EXTRA_PROPS"

java $ALL_PROPS -jar app.jar