# Fedora 4

[![Build Status](https://travis-ci.org/futures/fcrepo4.png?branch=master)](https://travis-ci.org/futures/fcrepo4)

System Requirements
* Java 7
* Maven 3
* A Servlet 3.0 container (e.g. Tomcat 7 or Jetty 8)


[JavaDocs](http://docs.fcrepo.org/) | 
[Fedora Futures Wiki](https://wiki.duraspace.org/display/FF/Fedora+Futures+Home) | 
[Use cases](https://wiki.duraspace.org/display/FF/Use+Cases)

Technical goals:
* Improved scalability and performance
* More flexible storage options
* Improved reporting and metrics
* Improved durability

## Running fcrepo4

```bash
$ mvn install
$ cd fcrepo-webapp
$ MAVEN_OPTS="-Xmx512m" mvn jetty:run
```

That's it! Your Fedora repository is up and running at: [http://localhost:8080/rest/](http://localhost:8080/rest/)

