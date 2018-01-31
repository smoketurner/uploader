Uploader
========
[![Build Status](https://travis-ci.org/smoketurner/uploader.svg?branch=master)](https://travis-ci.org/smoketurner/uploader)
[![Coverage Status](https://coveralls.io/repos/smoketurner/uploader/badge.svg)](https://coveralls.io/r/smoketurner/uploader)
[![Maven Central](https://img.shields.io/maven-central/v/com.smoketurner.uploader/uploader-application.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.smoketurner.uploader/uploader-application/)
[![GitHub license](https://img.shields.io/github/license/smoketurner/uploader.svg?style=flat-square)](https://github.com/smoketurner/uploader/tree/master)
[![Become a Patron](https://img.shields.io/badge/Patron-Patreon-red.svg)](https://www.patreon.com/jplock)

Uploader will listen on a TCP port (optionally using TLS) and batch and upload data to AWS S3. Data received will be split in newlines and then batched together, compressed and uploaded.

Installation
------------
To build this code locally, clone the repository then build the jar:
```
git clone https://github.com/smoketurner/uploader.git
cd uploader
./mvnw package
java -jar target/uploader-application-1.0.1-SNAPSHOT.jar server config.yml
```

The Uploader service should be listening on port `8443` for upload data and `8080` for API requests, and Dropwizard's administrative interface is available at `/admin` (both of these ports can be changed in the `config.yml` configuration file).


Support
-------

Please file bug reports and feature requests in [GitHub issues](https://github.com/smoketurner/uploader/issues).


License
-------

Copyright (c) 2018 Smoke Turner, LLC

This library is licensed under the Apache License, Version 2.0.

See http://www.apache.org/licenses/LICENSE-2.0.html or the [LICENSE](LICENSE) file in this repository for the full license text.
