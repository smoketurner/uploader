#!/bin/sh

echo 'Generate CA key'
openssl genrsa -aes256 -out ca.key 4096

echo 'Generate CA x509 certificate'
openssl req -new -x509 -sha256 -days 730 -key ca.key -out ca.crt

echo 'Generating server key'
openssl genrsa -out server.key 2048

echo 'Generating server CSR'
openssl req -new -key server.key -sha256 -out server.csr

echo 'Generating server x509 certificate'
openssl x509 -req -days 365 -sha256 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 1 -out server.crt

echo 'Convert server key to PKCS#8'
openssl pkcs8 -topk8 -inform PEM -outform PEM -in server.key -out server.key

echo 'Generating client key'
openssl genrsa -out client.key 2048

echo 'Generating client CSR'
openssl req -new -key client.key -out client.csr

echo 'Generating client x509 certificate'
openssl x509 -req -days 365 -sha256 -in client.csr -CA ca.crt -CAkey ca.key -set_serial 2 -out client.crt
