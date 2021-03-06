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
MQTT_BROKER_HOST_INTERNAL="${MQTT_SERVER_INTERNAL}"
MQTT_BROKER_HOST_EXTERNAL="${MQTT_SERVER_EXTERNAL}"
PROV_SERVER_HOST="${PROV_SERVER}"
SSC_SERVER_HOST="${SSC_SERVER}"
ALL_IN_ONE_HOST="${INTEGRATED_SERVER}"
FILE_STORE_DIRECTORY="${FILE_STORE_DIRECTORY_INTERNAL:=/tmp/filestore}"
FILE_STORE_EXTERNAL_URL="${FILE_STORE_URL}"
MQTT_BROKER_EXTERNAL_PORT="${MQTT_BROKER_EXTERNAL_PORT}"
OVSDB_EXTERNAL_PORT="${OVSDB_EXTERNAL_PORT}"
OFF_CHANNEL_REPORTING_INTERVAL_SECONDS="${OFF_CHANNEL_REPORTING_INTERVAL_SECONDS:=120}"
REPORTING_INTERVAL_SECONDS="${REPORTING_INTERVAL_SECONDS:=60}"




OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.managerAddr=$OVSDB_MANAGER_HOST"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.trustStore=/opt/tip-wlan/certs/truststore.jks"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.keyStore=/opt/tip-wlan/certs/server.pkcs12"
OVSDB_PROPS+=" -Dtip.wlan.ovsdb.configFileName=/app/opensync/config_2_ssids.json"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.address.internal=$MQTT_BROKER_HOST_INTERNAL"
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.address.external=$MQTT_BROKER_HOST_EXTERNAL"
MQTT_PROPS+=" -Dtip.wlan.ovsdb.externalPort=$OVSDB_EXTERNAL_PORT"
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.externalPort=$MQTT_BROKER_EXTERNAL_PORT"
MQTT_PROPS+=" -Dtip.wlan.mqttBroker.listenPort=1883"

LOGBACK_CONFIG_FILE="${LOGBACK_CONFIG_FILE:=/app/opensync/logback.xml}"
LOGGING_PROPS=" -Dlogging.config=file:$LOGBACK_CONFIG_FILE"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=9096 -Dtip.wlan.secondaryPort=9097"

SPRING_EXTRA_PROPS=" --add-opens java.base/java.lang=ALL-UNNAMED"

HOST_PROPS=" "

## These properties are used by the Routing Service and the values will be
## overridden in Helm chart to the IP-Address of running opensync-gw pod
## If OVSDB_MANAGER_IP variable is not defined, these properties default
## to the Hostname of the container
if [[ -n ${OVSDB_MANAGER_IP} ]]
then
  HOST_PROPS+=" -Dtip.wlan.externalHostName=${OVSDB_MANAGER_IP}"
  HOST_PROPS+=" -Dtip.wlan.internalHostName=${OVSDB_MANAGER_IP}"
  HOST_PROPS+=" -Dtip.wlan.introspectTokenApi.host=${OVSDB_MANAGER_IP}:9096"
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
  # SSC URLs
  HOST_PROPS+=" -Dtip.wlan.cloudEventDispatcherBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.statusServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.routingServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.alarmServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.systemEventServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.clientServiceBaseUrl=$SSC_URL"
  HOST_PROPS+=" -Dtip.wlan.serviceMetricServiceBaseUrl=$SSC_URL"

  # PROV URLs
  HOST_PROPS+=" -Dtip.wlan.customerServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.portalUserServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.firmwareServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.locationServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.manufacturerServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.equipmentServiceBaseUrl=$PROV_URL"
  HOST_PROPS+=" -Dtip.wlan.profileServiceBaseUrl=$PROV_URL"

DEFAULT_BRIDGE="${DEFAULT_BRIDGE:=lan}"
DEFAULT_WAN_TYPE="${DEFAULT_WAN_TYPE:=bridge}"
DEFAULT_LAN_TYPE="${DEFAULT_LAN_TYPE:=bridge}"
DEFAULT_LAN_NAME="${DEFAULT_LAN_NAME:=lan}"
DEFAULT_WAN_NAME="${DEFAULT_WAN_NAME:=wan}"


AUTO_PROV_CUSTOMER_ID="${AUTO_PROV_CUSTOMER_ID:=2}"
 
PROV_PROPS=" "
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_bridge=$DEFAULT_BRIDGE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_wan_type=$DEFAULT_WAN_TYPE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_wan_name=$DEFAULT_WAN_NAME"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_lan_type=$DEFAULT_LAN_TYPE"
PROV_PROPS+=" -Dtip.wlan.ovsdb.wifi-iface.default_lan_name=$DEFAULT_LAN_NAME"
PROV_PROPS+=" -Dtip.wlan.defaultOffChannelReportingIntervalSeconds=$OFF_CHANNEL_REPORTING_INTERVAL_SECONDS"
PROV_PROPS+=" -Dtip.wlan.defaultReportingIntervalSeconds=$REPORTING_INTERVAL_SECONDS"

PROV_PROPS+=" -Dtip.wlan.ovsdb.autoProvisionedCustomerId=$AUTO_PROV_CUSTOMER_ID"

FILE_STORE_PROPS=" "
FILE_STORE_PROPS+=" -Dtip.wlan.fileStoreDirectory=$FILE_STORE_DIRECTORY"
FILE_STORE_PROPS+=" -Dtip.wlan.externalFileStoreURL=$FILE_STORE_EXTERNAL_URL"

REMOTE_DEBUG_PORT=${REMOTE_DEBUG_PORT:-'5005'}
if [ "x$REMOTE_DEBUG_ENABLE" == "xtrue" ]
then
  REMOTE_DEBUG=" -agentlib:jdwp=transport=dt_socket,server=y,address=*:$REMOTE_DEBUG_PORT,suspend=n"
else
  REMOTE_DEBUG=" "
fi

JVM_EXTRA_PROPS=" ${JVM_MEM_OPTIONS:- } "

export ALL_PROPS="$JVM_EXTRA_PROPS $PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS $SPRING_EXTRA_PROPS $HOST_PROPS $PROV_PROPS $FILE_STORE_PROPS $REMOTE_DEBUG"

java $ALL_PROPS -jar app.jar
