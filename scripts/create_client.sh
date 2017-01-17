#!/usr/bin/env bash

set -euf -o pipefail

echo "Generating $1 key"
openssl genrsa -out $1.key 2048

echo "Generating $1 CSR"
openssl req -new -subj "/CN=$1" -key $1.key -out $1.csr

echo "Generating $1 x509 certificate"
openssl x509 -req -days 365 -sha256 -in $1.csr -CA ca.crt -CAkey ca.key -set_serial 2 -out $1.crt

echo "Generating $1 PKCS12 certificate"
openssl pkcs12 -export -inkey $1.key -in $1.crt -out $1.p12 -passout pass:changeit
