#!/bin/sh

VERSION="1.0.0-SNAPSHOT"

docker build --build-arg VERSION=${VERSION} -t "smoketurner/uploader:${VERSION}" .
