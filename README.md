# Fedora 4

[![Build Status](https://travis-ci.org/futures/fcrepo4.png?branch=master)](https://travis-ci.org/futures/fcrepo4)

[JavaDocs](http://docs.fcrepo.org/) | 
[Fedora Wiki](https://wiki.duraspace.org/display/FF) | 
[Use cases](https://wiki.duraspace.org/display/FF/Use+Cases)

Technical goals:
* Improved scalability and performance
* More flexible storage options
* Improved reporting and metrics
* Improved durability

## Building & running fcrepo4 from source

System Requirements
* Java 7
* Maven 3

```bash
$ git clone https://github.com/futures/fcrepo4.git
$ cd fcrepo4
$ MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=1024m" mvn install
$ cd fcrepo-webapp
$ MAVEN_OPTS="-Xmx512m" mvn jetty:run
```

### Jacoco Properties
The Properties passed to the JVM used by the JaCoCo code coverage plugin can be set via 
`jacoco.agent.it.arg` for integration tests  and `jacoco.agent.ut.arg` for unit tests:

```bash
$ MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=1024m" mvn -Djacoco.agent.it.arg="-XX:MaxPermSize=1024m -Xmx1024m" -Djacoco.agent.ut.arg="-XX:MaxPermSize=256m -Xmx1024m"  clean install
```


That's it! Your Fedora repository is up and running at: [http://localhost:8080/rest/](http://localhost:8080/rest/)

