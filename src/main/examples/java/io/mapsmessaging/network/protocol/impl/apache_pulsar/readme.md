<h1>Proof of Concept </h1>
This is a proof of concept that a 3rd party client library can be integrated into the server and enable bidirectional communications


To build you will need to add the dependency for the apache-pulsar client to the pom.xml to compile the classes

    <!-- https://mvnrepository.com/artifact/org.apache.pulsar/pulsar-client -->
    <dependency>
      <groupId>org.apache.pulsar</groupId>
      <artifactId>pulsar-client</artifactId>
      <version>2.7.0</version>
    </dependency>


Then edit the java services file in resources

<code>
resources/META-INF.services/io.mapsmessaging.network.protocol.ProtocolImplFactory
</code>

Add the following line 

<code>
io.mapsmessaging.network.protocol.impl.apache_pulsar.PulsarProtocolFactory
</code>

This will enable the server to load and access the pulsar client and you can then enable bi-directional connections between the server and the apache-pulsar server.


<code>
NetworkConnectionManager:

    global:

    data:
      -
          name: Connects to a Apache Pulsar server
          url: "noop://pulsar_host:pulsar_port/"
          protocol: pulsar
          username: user1
          password: password1
          sessionId: pulsarExample
          links:
            -
              direction: pull
              remote_namespace: persistent://public/default/my-topic
              local_namespace: /apache_pulsar
            -
              direction: push
              remote_namespace: return-topic
              local_namespace: /apache_pulsar
</code>