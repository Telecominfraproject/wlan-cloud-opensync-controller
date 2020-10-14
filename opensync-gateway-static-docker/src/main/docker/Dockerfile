FROM adoptopenjdk/openjdk13:alpine-jre
MAINTAINER TipWlan

# Update the package list and upgrade installed packages
RUN apk update && apk upgrade && apk add bash
RUN mkdir /app && mkdir /app/logs && mkdir /app/opensync
RUN mkdir -p /opt/tip-wlan/certs

COPY maven/app.jar /app
COPY app/opensync/logback.xml /app/opensync/logback.xml
COPY app/run.sh /app
COPY app/opensync/EquipmentExample.json /app/opensync/EquipmentExample.json
COPY app/opensync/ProfileAPExample.json /app/opensync/ProfileAPExample.json
COPY app/opensync/ProfileSsid.json /app/opensync/ProfileSsid.json
COPY app/opensync/LocationBuildingExample.json /app/opensync/LocationBuildingExample.json
COPY app/opensync/ProfileMetrics.json /app/opensync/ProfileMetrics.json
COPY app/opensync/ProfileRf.json /app/opensync/ProfileRf.json
COPY app/opensync/ProfileHotspot20.json /app/opensync/ProfileHotspot20.json
COPY app/opensync/ProfileOperator.json /app/opensync/ProfileOperator.json
COPY app/opensync/ProfileVenue.json /app/opensync/ProfileVenue.json
COPY app/opensync/ProfileIdProvider.json /app/opensync/ProfileIdProvider.json
COPY app/opensync/ProfileBonjour.json /app/opensync/ProfileBonjour.json

COPY app/commit.properties /app
RUN chmod +x /app/run.sh

WORKDIR /app
CMD ["/app/run.sh"]