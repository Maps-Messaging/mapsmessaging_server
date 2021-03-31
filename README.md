# MAPS Messaging Server

[MAPS(Multi Adapter and Protocol Standards)messaging server](https://www.mapsmessaging.io/)


## Introduction
Wire protocol standardisation for messaging has enabled an unprecidented interoperability for both clients and servers. It has come with the promise of no vendor lock in, healthy competition and the ability to swap out clients and servers as requirements change. While this is a noble approach, and has to some extent been fulfilled, there is an increasing realisation that as protocols evolve or new protocols are ratified the promise of interoperability is not attainable.
Take for example MQTT [V3.1.1](https://docs.oasis-open.org/mqtt/mqtt/v3.1.1/mqtt-v3.1.1.html) and [V5](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html), while evolution of the same protocol the functionality being offered in V5 far exceeds what was available in V3. Add to this AMQP V1.0, which is gaining ground in IoT, when trying to architect a coherent messaging fabric it requires multiple messaging servers with standalone integration systems or messaging servers with plugins that have limited support of the protocol. This results in complex deployments, rigid client and server deployments and not achieving the utopia that open wire protocols were promising to deliver.

## Protocol Support
With mapsmessaging server, the entire purpose is to support all of the open messaging standards in their entirety and to facilitate message flow between different protocols and versions seamlessly out of the box.
Current support:

| Protocol | Version | Support Status | WebSocket | Secure WebSockets | TCP | SSL | UDP | LoRa :red_circle: | Serial |
| -------- | ------- | -------------- | --------- | ----------------- | --- | --- | --- | ---- | ------ |
| Stomp    | 1.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:  | :heavy_check_mark:| :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| Stomp    | 1.2     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 3.1     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 3.1.1   | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT     | 5.0     | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| MQTT-SN  | 1.2     | :heavy_check_mark: | :x: | :x: | :x: | :x: | :heavy_check_mark: | :heavy_check_mark: | :x: |
| AMQP     | 1.0     | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| JMS-AMQP :small_orange_diamond: | JMS 2.0  | :heavy_check_mark: :small_blue_diamond: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :x: | :heavy_exclamation_mark: | :small_red_triangle: |
| NMEA     | 0183    | :heavy_check_mark: | :x: | :x: | :x: | :x: | :x: | :x: | :heavy_check_mark: |


:heavy_check_mark: - Tested, has conformance tests for validity of the protocol \
:small_red_triangle: - Not tested \
:small_blue_diamond: - Conformance tests under development \
:small_orange_diamond: - Using the QPID [JMS2.0 over AMQP client](https://qpid.apache.org/components/jms/index.html) \
:red_circle: - Requires appropriate hardware for support \
:heavy_exclamation_mark: - Transport does not support a connection based protocol \
:x: - The protocol and the transport are not compatible \
\
These protocols and transports are supported natively by the server,
