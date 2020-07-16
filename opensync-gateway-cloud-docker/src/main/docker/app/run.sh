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
CLIENT_MQTT_SSL_PROPS+=" -Dtip.wlan.mqttBroker.password=admin"

OVSDB_MANAGER_HOST=${OVSDB_MANAGER}
MQTT_BROKER_HOST="${MQTT_SERVER}"
PROV_SERVER_HOST="${PROV_SERVER}"
SSC_SERVER_HOST="${SSC_SERVER}"
ALL_IN_ONE_HOST="${INTEGRATED_SERVER}"

OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.managerAddr=$OVSDB_MANAGER_HOST"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.trustStore=/opt/tip-wlan/certs/truststore.jks"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.keyStore=/opt/tip-wlan/certs/server.pkcs12"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.configFileName=/app/opensync/config_2_ssids.json"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.address=$MQTT_BROKER_HOST"
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.listenPort=1883"

LOGBACK_CONFIG_FILE="${LOGBACK_CONFIG_FILE:=/app/opensync/logback.xml}"
LOGGING_PROPS=" -Dlogging.config=file:$LOGBACK_CONFIG_FILE"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443 -Dtip.wlan.secondaryPort=444"

SPRING_EXTRA_PROPS=" --add-opens java.base/java.lang=ALL-UNNAMED"

HOST_PROPS=" "
HOST_PROPS+=" -Dtip.wlan.introspectTokenApi.host=${OVSDB_MANAGER_HOST}:444"

## These properties are used by the Routing Service and the values will be
## overridden in Helm chart to the IP-Address of running opensync-gw pod
## If OVSDB_MANAGER_IP variable is not defined, these properties default
## to the Hostname of the container
if [[ -n ${OVSDB_MANAGER_IP} ]]
then
  HOST_PROPS+=" -Dtip.wlan.externalHostName=${OVSDB_MANAGER_IP}"
  HOST_PROPS+=" -Dtip.wlan.internalHostName=${OVSDB_MANAGER_IP}"
fi

if [[ -n $PROV_SERVER_HOST && -n $SSC_SERVER_HOST ]]
then
  echo Use specifed local host
  SSC_URL=https://${SSC_SERVER_HOST}:9031
  PROV_URL=https://${PROV_SERVER_HOST}:9091
else
  echo Its an Integrated server environment
  SSC_URL=https://${ALL_IN_ONE_HOST}:9092
  PROV_URL=https://${ALL_IN_ONE_HOST}:9092
fi
  // SSC URLs
  HOST_PROPS+=" -Dtip.wlan.cloudEventDispatcherBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.statusServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.routingServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.alarmServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.systemEventServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.profileServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.clientServiceBaseUrl=$SSC_URL"

  // PROV URLs
  HOST_PROPS+=" -Dtip.wlan.customerServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.portalUserServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.firmwareServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.serviceMetricServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.locationServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.manufacturerServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.equipmentServiceBaseUrl=$PROV_URL"

DEFAULT_BRIDGE="${DEFAULT_BRIDGE:=br-lan}"
DEFAULT_WAN_TYPE="${DEFAULT_WAN_TYPE:=eth}"
DEFAULT_LAN_TYPE="${DEFAULT_LAN_TYPE:=bridge}"
DEFAULT_LAN_NAME="${DEFAULT_LAN_NAME:=lan}"


AUTO_PROV_CUSTOMER_ID="${AUTO_PROV_CUSTOMER_ID:=2}"
 
PROV_PROPS=" "
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_bridge=$DEFAULT_BRIDGE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_wan_type=$DEFAULT_WAN_TYPE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_lan_type=$DEFAULT_LAN_TYPE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_lan_name=$DEFAULT_LAN_NAME"

PROV_PROPS+=" -Dtip.wlan.ovsdb.autoProvisionedCustomerId=$AUTO_PROV_CUSTOMER_ID"

export ALL_PROPS="$PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS $SPRING_EXTRA_PROPS $HOST_PROPS $PROV_PROPS"

java $ALL_PROPS -jar app.jar