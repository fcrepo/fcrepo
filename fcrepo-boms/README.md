## FcRepo4 BOMs

FcRepo4 provides a couple of Maven Bills of Material (BOMs) that make it easier to use FcRepo4 as a dependency in your own applications.

The primary one for building an application based on FcRepo4 is `fcrepo4-bom`.  There is also a smaller one, `fcrepo-jcr-bom`, which should not be needed if `fcrepo4-bom` is used.  `fcrepo-jcr-bom` is mostly for use by the fcrepo4 project itself.

See the README.md file in each of the BOM modules for more information on how to use them in your projects.

Maven also provides information on BOMs in the "Importing Dependencies" section of their [Introduction to Dependency Mechanism](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Importing_Dependencies).