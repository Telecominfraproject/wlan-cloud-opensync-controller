SSL_PROPS=" "
SSL_PROPS+=" -Dssl.props=file:///home/ec2-user/opensync/ssl.properties"
SSL_PROPS+=" -Dwhizcontrol.httpClientConfig=file:///home/ec2-user/opensync/httpClientConfig.json"

CLIENT_MQTT_SSL_PROPS=" "
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStore=/home/ec2-user/opensync/client2Keystore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.keyStorePassword=mypassword"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStore=/home/ec2-user/opensync/truststore.jks"
CLIENT_MQTT_SSL_PROPS+=" -Djavax.net.ssl.trustStorePassword=mypassword"

OVSDB_PROPS=" "
OVSDB_PROPS+=" -Dconnectus.ovsdb.managerAddr=3.88.149.10"
OVSDB_PROPS+=" -Dconnectus.ovsdb.listenPort=6640 "
OVSDB_PROPS+=" -Dconnectus.ovsdb.redirector.listenPort=6643"
OVSDB_PROPS+=" -Dconnectus.ovsdb.timeoutSec=30"
OVSDB_PROPS+=" -Dconnectus.ovsdb.trustStore=/home/ec2-user/opensync/truststore.jks"
OVSDB_PROPS+=" -Dconnectus.ovsdb.keyStore=/home/ec2-user/opensync/server.p12"

MQTT_PROPS=" "
MQTT_PROPS+=" -Dconnectus.mqttBroker.address=testportal.123wlan.com"
MQTT_PROPS+=" -Dconnectus.mqttBroker.listenPort=1883"

LOGGING_PROPS=" -Dlogging.config=file:///home/ec2-user/opensync/logback.xml"

RESTAPI_PROPS=" "
RESTAPI_PROPS+=" -Dserver.port=443"

export ALL_PROPS="$SSL_PROPS $CLIENT_MQTT_SSL_PROPS $OVSDB_PROPS $MQTT_PROPS $LOGGING_PROPS $RESTAPI_PROPS"

sudo java $ALL_PROPS -jar opensync_experiment-0.0.1-SNAPSHOT.jar > stdout.out 2>&1 &
