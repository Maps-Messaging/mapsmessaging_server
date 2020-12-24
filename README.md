# protocol_server
Multi Adapter and Protocol Standard messaging server


## Introduction
Wire protocol standards for messaging has enabled the messaging world to approach a plug and play approach to clients and servers, with the promise of no vendor lock in and ability to swap out clients / servers as requirements change. While this is a noble approach and has, to some extent, been realized, there is also an increasing realization that as protocol versions change or new ones are standardized that the promise of interoperability is not so attenable.

Take for example MQTT [V3.1.1](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html) and [V5](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html), while similar the functionality being offered in V5 far exceeds what was available in V3. Also add in that AMQP V1.0 has been ratified and is gaining grounds in IoT means that when trying to architect a coherent messaging fabric currently either requires multiple messaging servers with standalone integration systems or messaging servers with plugins that have limited support of the protocol. This results in potentially complex deployments, rigid client support and not what open wire protocols where meant to deliver.


## Protocol Support
With mapsmessaging server, the entire purpose of the server is to support all of the open standards, completely and allow for message flow between different protocols and versions seamlessly and without any additional servers or code. It currently supports, out of the box

| Protocol | Version | Support Status | WebSocket | Secure WebSockets | TCP | SSL | UDP | LoRa | Serial |
| -------- | ------- | -------------- | --------- | ----------------- | --- | --- | --- | ---- | ------ |
| Stomp    | 1.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  | :heavy_check_mark:| :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| Stomp    | 1.2     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 3.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: | 
| MQTT     | 3.1.1   | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 5.0     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT-SN  | 1.2     | :heavy_check_mark: | :x: | :x: | :x: | :x: | :heavy_check_mark: | :heavy_check_mark: | :x: |
| AMQP     | 1.0     | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| JMS-AMQP :small_orange_diamond: | JMS 2.0  | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |

:heavy_check_mark: - Tested, has conformance tests for validity of the protocol\
:small_red_triangle: - Not tested, but should work\
:heavy_exclamation_mark: - Transport does not really support a connection based protocol\
:small_blue_diamond: - Still building conformance tests to ensure correct integration\
:small_orange_diamond: - Using the QPID [JMS2.0 over AMQP client](https://qpid.apache.org/components/jms/index.html) 


