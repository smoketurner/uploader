#
# Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM openjdk:8-jdk-alpine AS BUILD_IMAGE

RUN apk add --no-cache curl openssl apr

WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn ./.mvn/

RUN ./mvnw install

COPY . .

RUN ./mvnw package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm target/original-*.jar && \
    mv target/*.jar app.jar

FROM openjdk:8-jre-alpine

ARG VERSION="1.0.0-SNAPSHOT"

LABEL name="uploader" version=$VERSION

ENV PORT 4433

RUN apk add --no-cache curl openssl apr

WORKDIR /app
COPY --from=BUILD_IMAGE /app/app.jar .
COPY --from=BUILD_IMAGE /app/config.yml .
COPY --from=BUILD_IMAGE /app/scripts/* ./scripts/

HEALTHCHECK --interval=10s --timeout=5s CMD curl --fail http://127.0.0.1:8180/healthcheck || exit 1

EXPOSE 4433 8443 8180

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
