FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.0.0-SNAPSHOT"

LABEL name="uploader" version=$VERSION

ENV PORT 8080

WORKDIR /app
COPY pom.xml .

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION" && \
    mvnw install

COPY . .

RUN mvn package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dmaven.source.skip=true && \
    rm target/original-*.jar && \
    mv target/uploader-application-1.0.0-SNAPSHOT.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf target && \
    apk del openjdk8

CMD java $JAVA_OPTS -Ddw.netty.listenPort=$PORT -jar app.jar server config.yml
