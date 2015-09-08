# Fedora 4

[![Build Status](https://travis-ci.org/fcrepo4/fcrepo4.png?branch=master)](https://travis-ci.org/fcrepo4/fcrepo4)

[JavaDocs](http://docs.fcrepo.org/) | 
[Fedora Wiki](https://wiki.duraspace.org/display/FF) | 
[Use cases](https://wiki.duraspace.org/display/FF/Use+Cases) |
[REST API](https://wiki.duraspace.org/display/FEDORA4x/RESTful+HTTP+API) |

Technical goals:
* Improved scalability and performance
* More flexible storage options
* Improved reporting and metrics
* Improved durability

## Building & running fcrepo4 from source

System Requirements
* Java 8
* Maven 3

```bash
$ git clone https://github.com/fcrepo4/fcrepo4.git
$ cd fcrepo4
$ MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=1024m" mvn install
$ cd fcrepo-webapp
$ MAVEN_OPTS="-Xmx512m" mvn jetty:run
```

Note: You may need to set the $JAVA_HOME property, since Maven uses it to find the Java runtime to use, overriding your PATH.
`mvn --version` will show which version of Java is being used by Maven, e.g.:

```bash
Java version: 1.8.0_31, vendor: Oracle Corporation
Java home: /usr/local/java-1.8.0_31/jre
```

To set your $JAVA_HOME environment variable:

```bash
JAVA_HOME=/path/to/java
```

If you have problems building fcrepo4 with the above settings, you may need to also pass
options to the JaCoCo code coverage plugin:

```bash
$ MAVEN_OPTS="-Xmx1024m" mvn -Djacoco.agent.it.arg="-XX:MaxMetaspaceSize=1024m -Xmx1024m" -Djacoco.agent.ut.arg="-XX:MaxMetaspaceSize=1024m -Xmx1024m"  clean install
```


That's it! Your Fedora repository is up and running at: [http://localhost:8080/rest/](http://localhost:8080/rest/)

