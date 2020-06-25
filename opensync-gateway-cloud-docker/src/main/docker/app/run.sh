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

OVSDB_MANAGER_HOST=${OVSDB_MANAGER}
MQTT_BROKER_HOST="${MQTT_SERVER}"

BACKEND_SERVER="${BACKEND_SERVER}"

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

LOGBACK_CONFIG_FILE="${LOGBACK_CONFIG_FILE:=/app/opensync/logback.xml}"
LOGGING_PROPS=" -Dlogging.config=file:$LOGBACK_CONFIG_FILE"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443 -Dtip.wlan.secondaryPort=444"

SPRING_EXTRA_PROPS=" --add-opens java.base/java.lang=ALL-UNNAMED"

HOST_PROPS=" "
HOST_PROPS+=" -Dtip.wlan.introspectTokenApi.host=${OVSDB_MANAGER_HOST}:444"

if [[ -n $BACKEND_SERVER ]]
then
  echo Use specifed local host

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
  HOST_PROPS+=" -Dtip.wlan.firmwareServiceBaseUrl=$HOST_URL"
  HOST_PROPS+=" -Dtip.wlan.manufacturerServiceBaseUrl=$HOST_URL"
fi

DEFAULT_BRIDGE="${DEFAULT_BRIDGE:=br-lan}"
AUTO_PROV_CUSTOMER_ID="${AUTO_PROV_CUSTOMER_ID:=2}"
 
PROV_PROPS=" "
PROV_PROPS+=" -Dconnectus.ovsdb.wifi-iface.default_bridge=$DEFAULT_BRIDGE"
PROV_PROPS+=" -Dconnectus.ovsdb.autoProvisionedCustomerId=$AUTO_PROV_CUSTOMER_ID"

export ALL_PROPS="$PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS $SPRING_EXTRA_PROPS $HOST_PROPS $PROV_PROPS"

java $ALL_PROPS -jar app.jar