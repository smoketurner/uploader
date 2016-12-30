#!/usr/bin/env bash

set -euf -o pipefail

echo 'Generate CA key'
openssl genrsa -aes256 -out ca.key 4096

echo 'Generate CA x509 certificate'
openssl req -new -x509 -sha256 -subj "/C=US/ST=Connecticut/L=Southport/O=Smoketurner/CN=ca" -days 730 -key ca.key -out ca.crt

echo 'Generating server key'
openssl genrsa -out server.key 2048

echo 'Generating server CSR'
openssl req -new -subj "/C=US/ST=Connecticut/L=Southport/O=Smoketurner/CN=localhost" -key server.key -sha256 -out server.csr

echo 'Generating server x509 certificate'
openssl x509 -req -days 365 -sha256 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 1 -out server.crt

echo 'Convert server key to PKCS#8'
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in server.key -out server_pkcs8.key

echo 'Generating client1 key'
openssl genrsa -out client1.key 2048

echo 'Generating client1 CSR'
openssl req -new -subj "/C=US/ST=Connecticut/L=Southport/O=Smoketurner/CN=client1" -key client1.key -out client1.csr

echo 'Generating client1 x509 certificate'
openssl x509 -req -days 365 -sha256 -in client1.csr -CA ca.crt -CAkey ca.key -set_serial 2 -out client1.crt
