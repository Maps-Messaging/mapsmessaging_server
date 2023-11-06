## Protocol Support

With the MapsMessaging server, the goal is to fully support all open messaging standards and facilitate seamless message flow between different protocols and versions right out of the box.

Current support:

| Protocol | Version | Support Status | WS  | WSS | TCP | SSL | UDP | DTLS | LoRa | Serial |
|----------|---------|----------------|-----|-----|-----|-----|-----|------|------|--------|
| Stomp    | 1.1     | Y              | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| Stomp    | 1.2     | Y              | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| MQTT     | 3.1     | Y              | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| MQTT     | 3.1.1   | Y              | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| MQTT     | 5.0     | Y              | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| MQTT-SN  | 1.2     | Y              | N   | N   | N   | N   | Y   | Y    | Y    | N      |
| MQTT-SN  | 2.0     | Y              | N   | N   | N   | N   | Y   | Y    | Y    | N      |
| CoAP     |         | Y              | N   | N   | N   | N   | Y   | Y    | Y    | N      |
| AMQP     | 1.0     | Y<sup>1</sup>  | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| JMS-AMQP | JMS 2.0 | Y<sup>1</sup>  | Y   | Y   | Y   | Y   | N   | N/A  | N/A  | N      |
| NMEA     | 0183    | Y              | N   | N   | Y   | N   | N   | N/A  | N    | Y      |
| Semtech  | 2       | Y              | N/A | N/A | N/A | N/A | Y   | N/A  | N/A  | N/A    |

- Y - Fully supported and tested with conformance tests for protocol validity.
- N - Not supported or not compatible.
- N/A - Transport does not support a connection-based protocol.
- <sup>1</sup> - Conformance tests are under development.

Using the QPID [JMS2.0 over AMQP client](https://qpid.apache.org/components/jms/index.html).

NMEA support requires appropriate hardware or configuration for GPSD socket connection.
