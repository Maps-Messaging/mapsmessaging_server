//
// Copyright [ 2020 - 2024 ] [Matthew Buckton]
// Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <SPI.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>

//---------------------------------------------------------------------------------------------------
// Define which Radio chip set to use
//
#define RH_69 1
//#define RH_95 1
//
//---------------------------------------------------------------------------------------------------

#if defined( RH_69 )
#include <RH_RF69.h>
//----------------------------
// Please set for your hardware
// for Adafruit FeatherM0 RFM69
// https://learn.adafruit.com/adafruit-feather-m0-radio-with-rfm69-packet-radio
//----------------------------
#define RFM_CS      8
#define RFM_RST     4
#define RFM_INT     7
#define LED        13
#define POWER      20
#define PACKET_LENGTH RH_RF69_MAX_MESSAGE_LEN
#define RADIO_FREQ 915.0
bool powerFlag = true;
#elif defined( RH_95 )
#include <RH_RF95.h>
//----------------------------
// Please set for your hardware
// for Adafruit Feather32u4 RFM9x
// https://learn.adafruit.com/adafruit-feather-32u4-radio-with-lora-radio-module/overview
//----------------------------
#define RFM_CS      8
#define RFM_RST     4
#define RFM_INT     7
#define LED        13
#define POWER      14
#define RADIO_FREQ 915.0
#define PACKET_LENGTH RH_RF95_MAX_PAYLOAD_LEN
bool powerFlag = false;
//----------------------------
#else
//----------------------------
// If using different hardware please refer to the
// RadioHead libraries and your hardware config
//----------------------------
#error Need to define the Radio Hardware to use
#endif
#include <RHReliableDatagram.h>

// Singleton instance of the radio driver
uint8_t LORA_GATEWAY = 2;
uint8_t MY_ADDRESS = 20;
uint8_t packet [PACKET_LENGTH];

//-------------------------------------------------------
// MQTT-SN Specific 
//
#define MQTT_SN_PUBLISH_HEADER_SIZE 7


//-------------------------------------------------------
// These need to be configured on the server to specific topic names
//-------------------------------------------------------
uint16_t sensorReadings   = 11;

//-------------------------------------------------------

#ifdef RH_69
RH_RF69 radio(RFM_CS, RFM_INT);
#endif

#ifdef RH_95
RH_RF95 radio(RFM_CS, RFM_INT);
#endif
RHReliableDatagram manager(radio, MY_ADDRESS);

//-------------------------------------------------------
// BME280 Specific
//
Adafruit_BME280 bme; // I2C

//-------------------------------------------------------
// Misc
//-------------------------------------------------------
unsigned long delayTime = 59000; // Offset so it slides off the minute

//-------------------------------------------------------
// This function is generic and builds a QoS:3 Publish
// frame specifically used by MQTT-SN only. It can be sent
// to any MQTT-SN server/gateway that has a server side
// topic registration that matches the topicId supplied.
//
// This is basically a session less publish
//-------------------------------------------------------
void packMQTT_SNPublish(short topicId, bool retain,  char* message, uint8_t messageLen){
  //
  // Init
  //
  
  byte flag = 0x60; // QoS == 3
  if(retain) {
    flag = flag | 0x10; // Set the Retain flag ( Bit:4)
  }
  uint8_t len = messageLen + MQTT_SN_PUBLISH_HEADER_SIZE;
  if(len > PACKET_LENGTH){
    Serial.println("Buffer larger than acceptable size");
    return;
  }
  //
  // Pack the buffer to send as a MQTT-SN Publish frame
  //
  packet[0] = len; // Packet Length: Exclude this
  packet[1] = 0x0C;  // MQTT-SN Publish request
  packet[2] = flag;
  packet[3] = (topicId & 0xff00) >> 8;
  packet[4] = topicId & 0xff;
  packet[5] = 0x0; // Must be 0 in QoS = 3 instance
  packet[6] = 0x0;
  for(int x=0;x<messageLen;x++){
    packet[x+MQTT_SN_PUBLISH_HEADER_SIZE] = message[x];
  }

  // Send Packet via Radio Head
  if(!manager.sendtoWait(packet, len, LORA_GATEWAY)){
    Serial.println("Failed to send/ No Ack");
  }
  else{
    Serial.println("Successfully received and Acked");
  }
}


void publishEvent() {

  float temp = 0.0;
  int count = 0;
  while (temp == 0.0 && count < 5) {
    temp = (float)bme.readTemperature();
    if (temp == 0.0) {
      delay(500);
    }
    count++;
  }
  float humidity = (float)bme.readHumidity();
  float pressure = (float)(bme.readPressure() / 100.0F);

  String send = "{ \"tem\":" + String(temp)+ "," +
                   "\"hum\":" + String(humidity) + "," +
                   "\"pre\":" + String(pressure) +" }";
  sendReading(sensorReadings, send);
}

void sendReading(uint16_t topic, String value){
  Serial.println("Sending:" + value);
  size_t len = value.length()+1;
  char* p = (char*) malloc(len);
  value.toCharArray(p, len);  
  packMQTT_SNPublish(topic, true, p, len);
  free(p);  
}

// --------------------------------------------------------------------------
// default settings
bool start_i2c() {
  Serial.println("Starting on i2c device 0x77");
  bool ret = bme.begin();
  if (ret) {
    Serial.println("I2C Bus Started");
  }
  return ret;
}

//---------------------------------------------------------------------------------------------------
// WIRELESS API
//
// NOTE: This will vary between hardware modules
//
void setupWireless(){
  pinMode(LED, OUTPUT);
  pinMode(RFM_RST, OUTPUT);
#ifdef RH_69
  Serial.println("Resetting RH_69");
  Serial.println();
  digitalWrite(RFM_RST, LOW);

  // manual reset
  digitalWrite(RFM_RST, HIGH);
  delay(10);
  digitalWrite(RFM_RST, LOW);
  delay(10);
#endif
//---------------------------------------------------------------------------------------------------
// NOTE: the RF69 and RF95 reset in opposite states
//---------------------------------------------------------------------------------------------------
#ifdef RH_95
  Serial.println("Resetting RH_95");
  digitalWrite(RFM_RST, HIGH);
  delay(100);

  // manual reset
  digitalWrite(RFM_RST, LOW);
  delay(10);
  digitalWrite(RFM_RST, HIGH);
  delay(10);
#endif

  if (!manager.init()) {
    Serial.println("Radio init failed");
    while (1);
  }
  Serial.println("Radio init OK!");

  if (!radio.setFrequency(RADIO_FREQ)) {
    Serial.println("setFrequency failed");
  }

  radio.setTxPower(POWER, powerFlag);  // range from 14-20 for power, 2nd arg must be true for 69HCW

#ifdef RH_69
  // The encryption key has to be the same as the one in the server
  uint8_t key[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16};
  radio.setEncryptionKey(key);
#endif
  Serial.print("RFM radio @");  Serial.print((int)RADIO_FREQ);  Serial.println(" MHz");
}

void setup() {
  Serial.begin(115200);
  Serial.println("Init() called");
  Serial.println("BME280 MQTT_SN Publish");

  if (!start_i2c()) {
    Serial.println("I2C bus failed to start");
  }
  setupWireless();
}

void loop() {
  publishEvent();
  delay(delayTime);  
}
