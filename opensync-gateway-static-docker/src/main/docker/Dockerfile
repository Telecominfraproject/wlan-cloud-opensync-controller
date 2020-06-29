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

RUN chmod +x /app/run.sh

WORKDIR /app
CMD ["/app/run.sh"]