FROM adoptopenjdk/openjdk13:alpine-jre

LABEL maintainer="ConnectUs.ai as part of the Telecom Infrastructure Project"
LABEL version="1.0"
LABEL description="This image runs the mqtt broker (mosquitto) and \
an OpenSync gateway."

# Update the package list and upgrade installed packages
RUN apk update && apk upgrade 
RUN mkdir -p /opt/tip-wlan/certs
VOLUME ["/opt/tip-wlan/certs"]

# Configure mqtt broker
RUN apk add mosquitto 
RUN mkdir -p /mosquitto/data /mosquitto/log
COPY app/mqtt/mosquitto.conf /etc/mosquitto/mosquitto.conf
VOLUME ["/mosquitto/data", "/mosquitto/log"]


# Configure OpenSync Gateway
RUN mkdir -p /app/logs /app/opensync /app/config
COPY maven/app.jar /app
COPY app/opensync/logback.xml /app/opensync/logback.xml
COPY app/run.sh /app
VOLUME ["/app/logs", "/app/config"]

RUN chmod +x /app/run.sh

EXPOSE 1883 6640 6643 4043

WORKDIR /app
ENTRYPOINT ["/app/run.sh"]
CMD [""]
