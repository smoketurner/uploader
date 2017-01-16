FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.0.0-SNAPSHOT"

LABEL name="uploader" version=$VERSION

ENV PORT 4433

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION"

WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn/

RUN ./mvnw install

COPY . .

RUN ./mvnw package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm target/original-*.jar && \
    mv target/uploader-application-1.0.0-SNAPSHOT.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf target && \
    apk del openjdk8

EXPOSE 4433 8443 8180

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
