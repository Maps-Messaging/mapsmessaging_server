This Arduino sketch acts as a gateway between the LoRa devices that send MQTT-SN packets and the Messaging server.

It supports

a) Multiple LoRa clients, specified by the Radio Head reliable packet address
b) MQTT-SN publishing of QoS:3 (-1) packets, as long as the TopicID is defined on the server

It has been tested on RF69 and RF95 chip sets from Adafruit ( See code for details ) and should work
with any supported RadioHead devices

