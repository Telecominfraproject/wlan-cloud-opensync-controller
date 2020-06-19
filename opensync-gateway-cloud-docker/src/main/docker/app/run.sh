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

OVSDB_MANAGER_HOST=opensync-controller
MQTT_BROKER_HOST=tip-wlan-opensync-mqtt-broke

BACKEND_SERVER="${BACKEND_SERVER}"

if [[ -n $BACKEND_SERVER ]]
then
  OVSDB_MANAGER_HOST=$BACKEND_SERVER
  MQTT_BROKER_HOST=$BACKEND_SERVER 
fi

OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dconnectus.ovsdb.managerAddr=$OVSDB_MANAGER_HOST"
OVSDB_PROPS+=" -Dconnectus.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dconnectus.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dconnectus.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dconnectus.ovsdb.trustStore=/opt/tip-wlan/certs/truststore.jks"
OVSDB_PROPS+=" -Dconnectus.ovsdb.keyStore=/opt/tip-wlan/certs/server.pkcs12"
OVSDB_PROPS+=" -Dconnectus.ovsdb.configFileName=/app/opensync/config_2_ssids.json"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dconnectus.mqttBroker.address=$MQTT_BROKER_HOST"
MQTT_PROPS+=" -Dconnectus.mqttBroker.listenPort=1883"

LOGGING_PROPS=" -Dlogging.config=file:/app/opensync/logback.xml"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443"

SPRING_EXTRA_PROPS=" --add-opens java.base/java.lang=ALL-UNNAMED"

HOST_PROPS=" "
if [[ -n $BACKEND_SERVER ]]
then
  echo Use specifed local host
  HOST_PROPS+=" -Dtip.wlan.externalHostName=$BACKEND_SERVER"
  HOST_PROPS+=" -Dtip.wlan.internalHostName=$BACKEND_SERVER"
  HOST_PROPS+=" -Dtip.wlan.introspectTokenApi.host=${BACKEND_SERVER}:9091"
  
  HOST_URL=https://${BACKEND_SERVER}:9092
  HOST_PROPS+=" -Dtip.wlan.cloudEventDispatcherBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.statusServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.routingServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.alarmServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.customerServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.locationServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.equipmentServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.profileServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.clientServiceBaseUrl=$HOST_URL"
fi
#echo HOST_PROPS $HOST_PROPS


export ALL_PROPS="$PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS $SPRING_EXTRA_PROPS $HOST_PROPS"

java $ALL_PROPS -jar app.jar