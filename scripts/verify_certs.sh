#!/bin/sh


echo '################# CA #################'
keytool -list -keystore truststore.jks -storepass changeit -alias root -v

echo '############## SERVER ################'
keytool -list -keystore keystore.jks -storepass changeit -alias 1 -v
