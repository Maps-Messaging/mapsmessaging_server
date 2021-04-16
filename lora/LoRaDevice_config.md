# LoRa device integration for the RaspberryPi

The MapsMessaging server has built in support for specific LoRa radioes, specifically the [Adafruit LoRa bonnet](https://www.adafruit.com/product/4074). The server comes with a library that integrates the LoRa device with the server. 

The server utilises the [RadioHead LoRa Library](https://www.airspayce.com/mikem/arduino/RadioHead/index.html) that offers addressable packets as well as reliable datagrams. The server uses this addressable packets as a UDP like network. 
In that the server will have an address, supplied in the URL portion of the configuration and then each device will have a unique address. Using this we can support 254 devices, though that is a large number but depending on radio traffic is fine for low traffic, 
if you want to support heavy radio traffic than a proper LoRa gateway device would be better suited ( on the road map ) 

## Prerequisites

There are a couple of prerequisites required to enable this feature

* RaspberryPi - Model 3 or better
* LoRa device - The one from Adafruit linked above is used locally but any RFM95 chipset should work, you may need to adjust the configuration for the device.
* MapsMessaging Server installation - Please check the [Installation Document](installation.md). The LoRa device library for the raspberryPi is packaged with the server.

## Configuration

Install the LoRa device as per the manufactures' recommendation, in the case of the Adafruit LoRa bonnet, refer to the [Adafruit Guide](https://learn.adafruit.com/adafruit-radio-bonnets).

Please note that the frequency of your device is dependent on your countries restrictions and ensure you have the correct frequency.

To configure the device ready for use with the server the first thing we need to do is to tell the server what the specific pinout is for the device. In the case of the Adafruit Bonnet they are

### Configuring the device
LoRaDevice.yaml
```yaml
LoRaDevice:
  global:
    power: 20 # TX Power to use
    frequency: 915.0   # Frequency to use, must be the band authorised for your country

    # ---------------------------------------------------------------------------------------------------------
    # Device specific values
    #
    # Generic name for this device, used in the NetworkManager.props to reference this device, any valid
    # UTF8 name can be used
    # ---------------------------------------------------------------------------------------------------------
  data:
    -
      name: loraDevice0
      cs: 7 # Chip Select pin used by the LoRa Radio Device
      irq: 22 # Interrupt pin used by the LoRa Radio device indicating available data
      rst: 25 # Reset Pin to use to reset the Radio Device, used during Startup to reset and clear the device
      radio: rfm95 # Radio Model, currently only RFM95 supported
      CADTimeout: 0  # A value of 0 is off, anything positive is then set
```

Here you will need to change the <u>frequency</u> to match your countries restrictions. The name is a unique name to identify this physical device since we could support more than 1 device on different pins on the RaspberryPi.

The server will then load the LoRaDevice library and attempt to connect to the physical device based on the configuration supplied. If successful a new device within the server will be available, in this case it will be called <b>loraDevice0</b>.

### Mapping the device to a protocol

The next step is to tell the server how you wish to use it, this is done in the [NetworkManager.yaml](/config/NetworkManager_Config.md) file. By adding the following snippet to the file the server will map MQTT-SN to the device and register specific MQTT-SN topics

```yaml
      -
        name: "LoRa device configuration"
        url: lora://loraDevice0:10/  # The port here maps to the MQTT-SN Address to use
        protocol: mqtt-sn
        auth: anon
        selectorThreadCount: 1
        registered: "*,12,/sensors/weather/indoor:*,11,/sensors/weather/outdoor:*,8,/sensors/water/garden:*,9,/sensors/water/house"
```

In the snippet above the server will create a new interface using loraDevice0, configured in the LoRaDevice.yaml, map node 10 for itself and then bind to the MQTT-SN protocol ready to process incoming packets.

#### MQTT-SN Registered Topics
The mqtt-sn registered event format is as a : separated csv format as follows

1. Node Address or * for all nodes
2. topic id - MQTT topic id sent from the client
3. topic name to map the event to 

Each configuration is separated by a :

## Checking the logs

If the server is unable to find the device or has issues loading the library you will see an entry in the log file such as

```log
2021-03-25 14:50:21, Network, , -, , [main] WARN  io.mapsmessaging.network.io.impl.lora.device.LoRaDeviceManager - LoRa device library failed to load no LoRaDevice in 
```

On a successful load and startup you should see something like

```log
2021-03-25 13:40:41, Engine, , -, , [main] WARN  io.mapsmessaging.MessageDaemon -               Loaded service lora, LoRa End Point Server Factory
```

## Example Arduino Code

There are examples for Arduino's that have a LoRa device 

* [Water Tank Ultrasonic sensor](https://github.com/Maps-Messaging/mapsmessaging_server/blob/main/src/main/arduino/examples/waterTankMonitor/waterTankMonitor.ino) 
* [Simple BME280 sensor](https://github.com/Maps-Messaging/mapsmessaging_server/blob/main/src/main/arduino/examples/bme280MQTT/bme280MQTT.ino)
* [Weather Station](https://github.com/Maps-Messaging/mapsmessaging_server/blob/main/src/main/arduino/examples/weatherStation/weatherStation.ino)

