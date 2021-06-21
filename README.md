# PLEASE BE ADVISED
The `main` branch of Fedora contains the "bleeding edge" of 6.0.0 development.  While we do not yet 
recommend it for production, it is ready for commmunity testing and feedback (which we welcome).

Updated: June 21, 2021

---

![Build](https://github.com/fcrepo/fcrepo/workflows/Build/badge.svg)

[JavaDocs](http://docs.fcrepo.org/) | 
[Fedora Wiki](https://wiki.lyrasis.org/display/FF) | 
[Use cases](https://wiki.lyrasis.org/display/FF/Use+Cases) |
[Technical Docs](https://wiki.lyrasis.org/display/FEDORA6x/) |
[REST API](https://wiki.lyrasis.org/display/FEDORA6x/RESTful+HTTP+API)

Fedora is a robust, modular, open source repository system for the management and dissemination of digital content.
It is especially suited for digital libraries and archives, both for access and preservation. It is also used to
provide specialized access to very large and complex digital collections of historic and cultural materials as well
as scientific data. Fedora has a worldwide installed user base that includes academic and cultural heritage
organizations, universities, research institutions, university libraries, national libraries, and government agencies.
The Fedora community is supported by the stewardship of the [Lyrasis](http://www.lyrasis.org) organization.

## Technical goals:
* Enhanced preservation sensibilities including preservation storage layer transparency
* Improved scalability and performance
* More flexible storage options
* Improved durability
* Improved reporting and metrics

## Downloads

The current web-deployable version of Fedora can be downloaded from the 
[Lyrasis website](https://wiki.lyrasis.org/display/FF/Downloads)
or from [Github](https://github.com/fcrepo/fcrepo/releases). These artifacts can be deployed directly in a Jetty or Tomcat container
as described in the guide to [deploying Fedora](https://wiki.lyrasis.org/display/FEDORA6x/Guides).

## Contributing

Contributions to the Fedora project are always welcome. These may take the form of testing the application, clarifying documentation
or writing code.

Code contributions will take the form of pull requests to this repository. They also require a signed
[contributor license agreement](https://wiki.lyrasis.org/display/DSP/Contributor+License+Agreements) on file before
a pull request can be merged. New developers may wish to review 
[this guide](https://wiki.lyrasis.org/display/FF/Guide+for+New+Developers)
as it explains both the process and standards for test coverage, style and documentation.

## Getting help

There are two community mailing lists where you can post questions or raise topics for discussion. Everyone is
welcome to subscribe and participate.

* https://groups.google.com/d/forum/fedora-community
* https://groups.google.com/d/forum/fedora-tech

Many of the developers are available on Slack in the  `#tech` and `bledding-edge` channels, hosted by [fedora-project
.slack.com](https://fedora-project.slack.com/).

In addition, there are weekly Zoom [technical calls](https://wiki.lyrasis.org/display/FF/Meetings) which anyone may
 join.

## Building and running Fedora from source

System Requirements
* Java 11
* Maven 3.6.3

```bash
$ git clone https://github.com/fcrepo/fcrepo.git
$ cd fcrepo
$ mvn install
```

The compiled Fedora war file can be found in `./fcrepo-webapp/target`. This can be deployed directly to a servlet container as
described in the [deployment guide](https://wiki.lyrasis.org/display/FEDORA6x/Deployment).


If deployed locally using a war file called `fcrepo.war`, the web application will typically be available at
http://localhost:8080/fcrepo/rest.

There are two convenient methods for *testing* the Fedora application by launching it directly from the command line.

One option is to use the "one click" application, which comes with an embedded Jetty servlet. This can be optionally built by running:

    mvn install -pl fcrepo-webapp -P one-click

and can be started by either double-clicking on the jar file or by running the following command:

    java -jar ./fcrepo-webapp/target/fcrepo-webapp-<version>-jetty-console.jar

By default, a Fedora home directory, `fcrepo`, is created in the current directory. You can change the default location by passing in an argument when starting the one-click, e.g.:

    java -Dfcrepo.home=/data/fedora-home -jar fcrepo-webapp-<version>-jetty-console.jar

An alternative is use the maven command: `mvn jetty:run`

```
$ cd fcrepo-webapp
$ mvn jetty:run
```

For both of these methods, your Fedora repository will be available at: [http://localhost:8080/rest/](http://localhost:8080/rest/)

Note: You may need to set the $JAVA_HOME property, since Maven uses it to find the Java runtime to use, overriding your PATH.
`mvn --version` will show which version of Java is being used by Maven, e.g.:

```bash
Java version: 11.0.2, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/openjdk-11.0.2.jdk/Contents/Home
Default locale: en_US, platform encoding: UTF-8
```

To set your $JAVA_HOME environment variable:

```bash
export JAVA_HOME=/path/to/java
```

