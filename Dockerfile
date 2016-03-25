FROM java:openjdk-8-jre
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="uploader" version="1.0.0-SNAPSHOT"

RUN mkdir -p /opt
WORKDIR /opt
COPY ./uploader.jar /opt
COPY ./uploader-application/uploader.yml /opt
VOLUME ["/opt"]

EXPOSE 8080 8888
ENTRYPOINT ["java", "-d64", "-server", "-jar", "uploader.jar"]
CMD ["server", "uploader.yml"]
