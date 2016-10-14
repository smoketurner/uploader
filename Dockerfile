FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.0.0-SNAPSHOT"

LABEL name="uploader" version=$VERSION

ENV PORT 8080
ENV M2_HOME /usr/lib/mvn
ENV M2 $M2_HOME/bin
ENV PATH $PATH:$M2_HOME:$M2

WORKDIR /app
COPY . .

RUN apk add --no-cache curl openjdk8="$JAVA_ALPINE_VERSION" && \
    curl http://mirrors.sonic.net/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | tar -zx && \
    mv apache-maven-3.3.9 /usr/lib/mvn && \
    # build the application into a single JAR, including dependencies
    mvn package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dmaven.source.skip=true && \
    rm uploader-application/target/original-*.jar && \
    mv uploader-application/target/*.jar app.jar && \
    # remove all build artifacts & dependencies, Maven, and the JDK
    rm -rf /root/.m2 && \
    rm -rf /usr/lib/mvn && \
    rm -rf uploader-application/target && \
    apk del openjdk8

CMD java $JAVA_OPTS -Ddw.server.connector.port=$PORT -jar app.jar server config.yml
