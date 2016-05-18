FROM java:openjdk-8-jre
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.0.0-SNAPSHOT"

LABEL name="uploader" version=$VERSION

RUN mkdir -p /opt/uploader
WORKDIR /opt/uploader
COPY ./uploader.jar /opt/uploader
COPY ./uploader-application/uploader.yml /opt/uploader
VOLUME ["/opt/uploader"]

EXPOSE 8080 8888
ENTRYPOINT ["java", "-d64", "-server", "-jar", "uploader.jar"]
CMD ["server", "uploader.yml"]
