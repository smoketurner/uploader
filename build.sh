#!/bin/sh

VERSION="1.0.0-SNAPSHOT"

docker rm -f build-cont
docker build -t build-img -f Dockerfile.build .
docker create --name build-cont build-img
docker cp "build-cont:/src/uploader-application/target/uploader-application-${VERSION}.jar" ./uploader.jar
docker build --build-arg VERSION=${VERSION} -t "smoketurner/uploader:${VERSION}" .
