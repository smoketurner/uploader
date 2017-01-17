#!/bin/sh

openssl s_client -connect 127.0.0.1:4433 -cert client1.crt -key client1.key -CAfile ca.crt -tls1 "$@"
