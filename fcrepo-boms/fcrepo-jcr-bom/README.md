This fcrepo4 BOM was created so we could remove the separate fcrepo-jcr module.  It could also be expanded to include other dependencies in the JCR stack (so that they are managed from this single location).

To use it, include the following in the `dependencyManagement` section of your pom.xml file:

    <project>
      [...]
      <dependencyManagement>
        <dependencies>
          <dependency>
            <groupId>org.fcrepo</groupId>
            <artifactId>fcrepo-jcr-bom</artifactId>
            <version>${fcrepo.version}</version>
            <type>pom</type>
            <scope>import</scope>
          </dependency>
        </dependencies>
      </dependencyManagement>
      [...]
    </project>

Then you can use the individual dependencies, as needed, in your pom.xml's `dependencies` section; for instance:

    <project>
      [...]
      <dependencies>
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-jcr</artifactId>
        </dependency>
      </dependencies>
      [...]
    </project>

Maven also provides information on using BOMs in the "Importing Dependencies" section of their [Introduction to Dependency Mechanism](http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Importing_Dependencies).