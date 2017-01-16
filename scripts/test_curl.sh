#!/bin/sh

curl \
-X POST \
--cert client1.p12:changeit \
--cacert ca.crt \
--compressed \
-v \
https://127.0.0.1:8443/v1/batch
