PROFILES=" -Dspring.profiles.include=mqtt_receiver,ovsdb_redirector,ovsdb_manager"

SSL_PROPS=" "
SSL_PROPS+=" -Dssl.props=file:/app/certs/ssl.properties"
SSL_PROPS+=" -Dwhizcontrol.httpClientConfig=file:/app/certs/httpClientConfig.json"

CLIENT_MQTT_SSL_PROPS=" "
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStore=/app/certs/client_keystore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStorePassword=mypassword"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStore=/app/certs/truststore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStorePassword=mypassword"
CLIENT_MQTT_SSL_PROPS+=" -Dconnectus.mqttBroker.password=admin"

OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dconnectus.ovsdb.managerAddr=________"
OVSDB_PROPS+=" -Dconnectus.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dconnectus.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dconnectus.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dconnectus.ovsdb.trustStore=/app/certs/truststore.jks"
OVSDB_PROPS+=" -Dconnectus.ovsdb.keyStore=/app/certs/server.pkcs12"
OVSDB_PROPS+=" -Dconnectus.ovsdb.configFileName=/app/opensync/config_2_ssids.json"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dconnectus.mqttBroker.address=_________"
MQTT_PROPS+=" -Dconnectus.mqttBroker.listenPort=1883"

LOGGING_PROPS=" -Dlogging.config=file:/app/opensync/logback.xml"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443"

export ALL_PROPS="$PROFILES $SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS "

sudo java $ALL_PROPS -jar opensync-gateway-static-process-0.0.1-SNAPSHOT.jar > stdout.out 2>&1 &

