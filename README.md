# Fedora 4

[![Build Status](https://travis-ci.org/futures/fcrepo4.png?branch=master)](https://travis-ci.org/futures/fcrepo4)

System Requirements
* Java 7
* Maven 3


[Fedora4Lib](http://fedora4lib.org) | 
[JavaDocs](http://futures.github.com/fcrepo4) | 
[Fedora Futures Wiki](https://wiki.duraspace.org/display/FF/Fedora+Futures+Home) | 
[Use cases](https://wiki.duraspace.org/display/FF/Use+Cases)

Technical goals:
* Improved scalability and performance
* flexible storage options
* dynamic metadata
* globally unique and reliable identifiers
* improved audit trail for capturing lifecycle events

Other goals:
* straightforward API

## Running fcrepo4

```bash
$ mvn install
$ cd fcrepo-webapp
$ MAVEN_OPTS="-Xmx512m" mvn jetty:run
$ curl "http://localhost:8080/rest/describe"
```

