# Extensible JMS Selector Parser

JMS Selector parser, this is a 2 pass parser that compiles the selector to the simplest form for faster execution. All logical and mathematical operations are performed, where possible, during compilation resulting in a minimal set of operations required to be performed to evaluate the map.

The source can be found at the [github project](https://github.com/Maps-Messaging/jms_selector_parser) 

## pom.xml setup

Add the repository configuration into the pom.xml
``` xml
    <!-- MapsMessaging jfrog server -->
    <repository>
      <id>mapsmessaging.io</id>
      <name>artifactory-releases</name>
      <url>https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-mvn-prod</url>
    </repository>
```    

Then include the dependency
``` xml
    <!-- JMS Selector logic module -->
     <dependencies>    
        <dependency>
          <groupId>io.mapsmessaging</groupId>
          <artifactId>Extensible_JMS_Selector_Parser</artifactId>
          <version>1.0.0</version>
        </dependency>
     </dependencies>    
```

For examples and usage of the selector please review the [usage documentation](usage.md) to find out how to extend the syntax programmatically please check the [extension documentation](extensions.md)
