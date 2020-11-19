# PLEASE BE ADVISED
The `main` branch of Fedora contains the most current state of 6.0.0 development. It is nearing an alpha release and while not considered 
production ready, it is ready for community testing and feedback.

Updated: November 13, 2020

---

![Build](https://github.com/fcrepo/fcrepo/workflows/Build/badge.svg)

[JavaDocs](http://docs.fcrepo.org/) | 
[Fedora Wiki](https://wiki.duraspace.org/display/FF) | 
[Use cases](https://wiki.duraspace.org/display/FF/Use+Cases) |
[REST API](https://wiki.duraspace.org/display/FEDORA5x/RESTful+HTTP+API) |

Fedora is a robust, modular, open source repository system for the management and dissemination of digital content.
It is especially suited for digital libraries and archives, both for access and preservation. It is also used to
provide specialized access to very large and complex digital collections of historic and cultural materials as well
as scientific data. Fedora has a worldwide installed user base that includes academic and cultural heritage
organizations, universities, research institutions, university libraries, national libraries, and government agencies.
The Fedora community is supported by the stewardship of the [DuraSpace](http://www.duraspace.org) organization.

## Technical goals:
* Improved scalability and performance
* More flexible storage options
* Improved reporting and metrics
* Improved durability

## Downloads

The current web-deployable version of Fedora can be downloaded from the [Duraspace website](https://wiki.duraspace.org/display/FF/Downloads)
or from [Github](https://github.com/fcrepo/fcrepo/releases). These artifacts can be deployed directly in a Jetty or Tomcat container
as described in the guide to [deploying Fedora](https://wiki.duraspace.org/display/FEDORA5x/Deploying+Fedora+-+Complete+Guide).

## Contributing

Contributions to the Fedora project are always welcome. These may take the form of testing the application, clarifying documentation
or writing code.

Code contributions will take the form of pull requests to this repository. They also require a signed
[contributor license agreement](https://wiki.duraspace.org/display/DSP/Contributor+License+Agreements) on file before
a pull request can be merged. New developers may wish to review [this guide](https://wiki.duraspace.org/display/FF/Guide+for+New+Developers)
as it explains both the process and standards for test coverage, style and documentation.

## Getting help

There are two community mailing lists where you can post questions or raise topics for discussion. Everyone is
welcome to subscribe and participate.

* https://groups.google.com/d/forum/fedora-community
* https://groups.google.com/d/forum/fedora-tech

Many of the developers are available on the `#fcrepo` IRC channel, hosted by [freenode.net](http://webchat.freenode.net).

In addition, there are weekly [technical calls](https://wiki.duraspace.org/display/FF/Meetings) which anyone may join.

## Building and running Fedora from source

System Requirements
* Java 11
* Maven 3.6.3

```bash
$ git clone https://github.com/fcrepo/fcrepo.git
$ cd fcrepo
$ MAVEN_OPTS="-Xmx1024m -XX:MaxMetaspaceSize=1024m" mvn install
```

The compiled Fedora war file can be found in `./fcrepo-webapp/target`. This can be deployed directly to a servlet container as
described in the [deployment guide](https://wiki.duraspace.org/display/FEDORA5x/Deploying+Fedora+-+Complete+Guide).


If deployed locally using a war file called `fcrepo.war`, the web application will typically be available at
http://localhost:8080/fcrepo/rest.

There are two convenient methods for *testing* the Fedora application by launching it directly from the command line.

One option is to use the "one click" application, which comes with an embedded Jetty servlet. This can be optionally built by running:

    mvn install -pl fcrepo-webapp -P one-click

and can be started by either double-clicking on the jar file or by running the following command:

    java -jar ./fcrepo-webapp/target/fcrepo-webapp-<version>-jetty-console.jar

By default, a Fedora home directory, `fcrepo`, is created in the current directory. You can change the default location by passing in an argument when starting the one-click, e.g.:

    java -Dfcrepo.home=/data/fedora-home -jar fcrepo-webapp-6.0.0-SNAPSHOT-jetty-console.jar

An alternative is use the maven command: `mvn jetty:run`

```
$ cd fcrepo-webapp
$ MAVEN_OPTS="-Xmx512m" mvn jetty:run
```

For both of these methods, your Fedora repository will be available at: [http://localhost:8080/rest/](http://localhost:8080/rest/)

Note: You may need to set the $JAVA_HOME property, since Maven uses it to find the Java runtime to use, overriding your PATH.
`mvn --version` will show which version of Java is being used by Maven, e.g.:

```bash
Java version: 1.8.0_31, vendor: Oracle Corporation
Java home: /usr/local/java-1.8.0_31/jre
```

To set your $JAVA_HOME environment variable:

```bash
export JAVA_HOME=/path/to/java
```

If you have problems building fcrepo with the above settings, you may need to also pass
options to the JaCoCo code coverage plugin:

```bash
$ MAVEN_OPTS="-Xmx1024m" mvn -Djacoco.agent.it.arg="-XX:MaxMetaspaceSize=1024m -Xmx1024m" -Djacoco.agent.ut.arg="-XX:MaxMetaspaceSize=1024m -Xmx1024m"  clean install
```


