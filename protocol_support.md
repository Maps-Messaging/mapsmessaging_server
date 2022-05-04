## Protocol Support
With mapsmessaging server, the entire purpose is to support all the open messaging standards in their entirety and to facilitate message flow between different protocols and versions seamlessly out of the box.
Current support:

| Protocol | Version | Support Status | WebSocket | Secure WebSockets | TCP | SSL | UDP | LoRa :red_circle: | Serial |
| -------- |---------| -------------- | --------- | ----------------- | --- | --- | --- | ---- | ------ |
| Stomp    | 1.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  | :heavy_check_mark:| :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| Stomp    | 1.2     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 3.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 3.1.1   | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 5.0     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT-SN  | 1.2     | :heavy_check_mark: | :x: | :x: | :x: | :x: | :heavy_check_mark: | :heavy_check_mark: | :x: |
| MQTT-SN  | 2.0     | :heavy_check_mark: | :x: | :x: | :x: | :x: | :heavy_check_mark: | :heavy_check_mark: | :x: |
| AMQP     | 1.0     | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| JMS-AMQP :small_orange_diamond: | JMS 2.0 | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| NMEA :small_red_triangle_down:    | 0183    | :heavy_check_mark: | :x: | :x: | :heavy_check_mark: | :x: | :x: | :x: | :heavy_check_mark: |


:heavy_check_mark: - Tested, has conformance tests for validity of the protocol \
:small_red_triangle: - Not tested \
:small_blue_diamond: - Conformance tests under development \
:small_orange_diamond: - Using the QPID [JMS2.0 over AMQP client](https://qpid.apache.org/components/jms/index.html) \
:red_circle: - Requires appropriate hardware for support \
:heavy_exclamation_mark: - Transport does not support a connection based protocol \
:small_red_triangle_down: - NMEA requires either a GPS device or be configured for GPSD socket connection \
:x: - The protocol and the transport are not compatible \
\
These protocols and transports are supported natively by the server,
