## Protocol Support
With mapsmessaging server, the entire purpose is to support all the open messaging standards in their entirety and to facilitate message flow between different protocols and versions seamlessly out of the box.
Current support:

| Protocol                       | Version | Support Status | WS  | WSS | TCP    | SSL | UDP | DTLS | LoRa | Serial |
|--------------------------------|---------|----------------|-----|-----|--------| --- |----|------|-------| ------ |
| Stomp                          | 1.1  | Y              | Y   | Y           | Y      | Y | N  | N/A  | N/A   | 0 |
| Stomp                          | 1.2  | Y              | Y   | Y           | Y      | Y | N  | N/A  |N/A   | 0 |
| MQTT                           | 3.1  | Y              | Y   | Y           | Y      | Y | N  | N/A  |N/A   | 0 |
| MQTT                           | 3.1.1 | Y              | Y   | Y           | Y      | Y | N  | N/A  | N/A   | 0 |
| MQTT                           | 5.0  | Y              | Y   | Y           | Y      | Y | N  | N/A  | N/A   | 0 |
| MQTT-SN                        | 1.2  | Y              | N   | N           | N      | N | Y  | Y    | Y     | N |
| MQTT-SN                        | 2.0  | Y              | N   | N           | N      | N | Y  | Y    | Y     | N |
| CoAP                           |      | Y              | N   | N           | N      | N | Y  | Y    | Y     | N |
| AMQP                           | 1.0  | Y-1            | Y   | Y           | Y      | Y | N  | N/A  | N/A   | 0 |
| JMS-AMQP :small_orange_diamond: | JMS 2.0 | Y-1            | Y   | Y           | Y      | Y | N  | N/A  | N/A   | 0 |
| NMEA :small_red_triangle_down: | 0183 | Y              | N   | N           | Y      | N | N  | N/A  | N     | Y |


Y - Tested, has conformance tests for validity of the protocol \
0 - Not tested \
1 - Conformance tests under development \
:small_orange_diamond: - Using the QPID [JMS2.0 over AMQP client](https://qpid.apache.org/components/jms/index.html) \
:red_circle: - Requires appropriate hardware for support \
N/A - Transport does not support a connection based protocol \
:small_red_triangle_down: - NMEA requires either a GPS device or be configured for GPSD socket connection \
N - The protocol and the transport are not compatible \
\
These protocols and transports are supported natively by the server,
