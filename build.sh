#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker build --build-arg VERSION=${VERSION} -t "smoketurner/uploader:${VERSION}" .
