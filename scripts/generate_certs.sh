#!/usr/bin/env bash

set -euf -o pipefail

echo 'Removing old certificates'
rm *.crt *.key *.jks *.csr *.p12 || true

echo 'Generate CA key'
openssl genrsa -aes256 -out ca.key 4096

echo 'Generate CA x509 certificate'
openssl req -new -x509 -sha256 -subj "/CN=ca" -days 730 -key ca.key -out ca.crt

echo 'Import CA certificate into truststore'
keytool -importcert -file ca.crt -alias root -keystore truststore.jks -storepass changeit -noprompt

echo 'Generating server key'
openssl genrsa -out server.key 2048

echo 'Generating server CSR'
openssl req -new -subj "/CN=localhost" -key server.key -sha256 -out server.csr

echo 'Generating server x509 certificate'
openssl x509 -req -days 365 -sha256 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 1 -out server.crt

echo 'Creating server bundle certificate'
cat server.crt ca.crt > bundle.crt

echo 'Generating bundle PKCS12 certificate'
openssl pkcs12 -export -inkey server.key -in bundle.crt -out bundle.p12 -passout pass:changeit

echo 'Importing certificate bundle into keystore'
keytool -importkeystore -srckeystore bundle.p12 -srcstoretype PKCS12 -srcstorepass changeit -destkeystore keystore.jks -deststorepass changeit -noprompt
rm bundle.p12
rm bundle.crt

echo 'Convert server key to PKCS#8'
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key -out server_pkcs8.key
