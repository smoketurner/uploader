#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker run \
--name uploader \
--rm \
-e PORT=4433 \
-p 4433:4433 \
-p 8443:8443 \
smoketurner/uploader:${VERSION}
