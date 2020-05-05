FROM adoptopenjdk/openjdk13:alpine-jre
MAINTAINER ConnectUs

# Update the package list and upgrade installed packages
RUN apk update && apk upgrade && apk add bash
RUN mkdir /app && mkdir /app/logs && mkdir /app/opensync
RUN mkdir -p /opt/tip-wlan/certs

COPY maven/app.jar /app
COPY app/opensync/logback.xml /app/opensync/logback.xml
COPY app/run.sh /app

RUN chmod +x /app/run.sh

WORKDIR /app
CMD ["/app/run.sh"]