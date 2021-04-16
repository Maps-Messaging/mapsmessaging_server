# LoRa via a Serial Gateway

Sometimes it is not possible to run the physical LoRa device since you may not have a RaspberryPi. With MapsMessaging there is an Arduino LoRa gateway that can be used instead.
In these instances the Arduino acts as a LoRa gateway between the server and the LoRa devices.

## Prerequisites

* MapsMessaging Server installation - Please check the [Installation Document](installation.md). The LoRa device library for the raspberryPi is packaged with the server.
* Arduino with a LoRa device, for example, The [LoRa Feather](https://www.adafruit.com/product/3178) from Adafruit.
* Arduino IDE to build and load the software on the device.
* [LoRa Gateway Arduino code](https://github.com/Maps-Messaging/mapsmessaging_server/tree/main/src/main/arduino/LoRaMQTT_SNGateway)

## Configuration

The configuration for the gateway is done within the [NetworkManager.yaml](config/NetworkManager_Config.md) file and simply requires the following 

```yaml
     -
        name: "LoRa Serial gateway port"
        url: serial://ttyACM0,115200,8,N,1/?address=2&power=22
        protocol: Lora_MQTT-SN
        auth: anon
        selectorThreadCount: 1
        registered:  "*,12,/sensors/weather/indoor:*,11,/sensors/weather/outdoor:*,8,/sensors/water/garden:*,9,/sensors/water/house"
```

In the above the configuration tells the server that there is a device connected to ttyACM0 and that this device is bound to the protocol Lora_MQTT-SN. This protocol is an internal MapsMessaging protocol used to send/receive LoRa packets over the serial device.

### URL layout

Here the URL specifies a Serial connection using the device ttyACM0. The device name will vary depending on OS and gateway hardware. This is then followed by the serial configuration

* Baud Rate : 115200
* Bits : 8
* Parity : None
* Stop bits : 1

The rest of the url contains the address that the Gateway should use, and the Radio Power it should use.

### MQTT-SN Registered Topics
The mqtt-sn registered event format is as a : separated csv format as follows

1. Node Address or * for all nodes
2. topic id - MQTT topic id sent from the client
3. topic name to map the event to

Each configuration is separated by a :

## Installation

Once you have downloaded the gateway to a suitable Arduino simply connect the Arduino's USB to the server you wish to use it on, take note of the serial device that it appears on and ensure the configuration matches.

You should then see entries in the log file such as the following when the gateway detects valid packets

```log

2021-03-25 13:40:41, Engine, , -, , [main] WARN  io.mapsmessaging.MessageDaemon -               Loaded service Lora_MQTT-SN, LoRa MQTT-SN gateway via serial connection
2021-03-25 13:40:42, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-3] DEBUG LoRa Gateway on ttyACM0 - Gateway Ping received, Radio state is on = 1
2021-03-25 13:40:42, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-3] DEBUG LoRa Gateway on ttyACM0 - Gateway command executed successfully <13>
2021-03-25 13:40:42, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-3] DEBUG LoRa Gateway on ttyACM0 - Gateway command executed successfully <13>
2021-03-25 13:40:44, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-3] DEBUG LoRa Gateway on ttyACM0 - Gateway Ping received, Radio state is on = 1
2021-03-25 13:40:45, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-3] DEBUG LoRa Gateway on ttyACM0 - Gateway Ping received, Radio state is on = 1
2021-03-25 13:40:47, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-5] DEBUG LoRa Gateway on ttyACM0 - Gateway Ping received, Radio state is on = 1
....
2021-03-25 15:24:25, Protocol, LoRa MQTT-SN, LoRa MQTT-SN GW-1.0, LoRa MQTT-SN LoRa MQTT-SN GW, [ForkJoinPool-1-worker-5] DEBUG MQTT-SN 1.2 Protocol on LoRa MQTT-SN - Registered Event processed for /sensors/weather/outdoor

```
