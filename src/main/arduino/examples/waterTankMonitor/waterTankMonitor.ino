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

//---------------------------------------------------------------------------------------------------
// Define Trigger Pin, Echo Pin for ultrasonic and the reference and test pin to detect which board
//---------------------------------------------------------------------------------------------------
// Ultrasonic
#define TRIGGER_PIN 10
#define ECHO_PIN 12

// Board identifier tests
#define REFERENCE_PIN 23
#define TEST_PIN 22

// Battery level analog input
#define BATTERY_LEVEL_PIN  A0

#define BATTERY_BUILT_IN_PIN A9

#define DEBUG 1
//---------------------------------------------------------------------------------------------------

//---------------------------------------------------------------------------------------------------
// Include any required headers
#include <SPI.h>
#include <RH_RF95.h>
#include <RHReliableDatagram.h>
#include <Adafruit_SleepyDog.h>
//---------------------------------------------------------------------------------------------------

//----------------------------
// Please set for your hardware
// for Adafruit Feather32u4 RFM9x
// https://learn.adafruit.com/adafruit-feather-32u4-radio-with-lora-radio-module/overview
//----------------------------
#define RFM_CS      8
#define RFM_RST     4
#define RFM_INT     7
#define RADIO_FREQ 915.0
#define PACKET_LENGTH RH_RF95_MAX_PAYLOAD_LEN
#define MQTT_SN_PUBLISH_HEADER_SIZE 7

//---------------------------------------------------------------------------------------------------
// Define any global variables to use and set values, if required
// Every 15 minutes, no need to flood airwaves with the water level
const unsigned long minute = 60000;
unsigned long longDelay = 15 * minute;  //  15 Minutes

// Take 3 readings and average out the readings
const int READINGS = 3;

// LoRa radio configurations
const bool powerFlag = false;

// Local device address
uint8_t deviceAddress = 16;

// Gateway Server address
uint8_t LoRaGateway = 2;

// The MQTT-SN topic number
uint16_t topicNumber   = 9;

// Max size of the packet
uint8_t packet [PACKET_LENGTH];

const uint8_t MAX_POWER = 20;
const uint8_t MIN_POWER = 14;
uint8_t currentPower = MIN_POWER;

unsigned long loopCounter = 0;

RH_RF95 radio(RFM_CS, RFM_INT);
RHReliableDatagram manager(radio, deviceAddress);

//---------------------------------------------------------------------------------------------------
// This function is generic and builds a QoS:3 Publish
// frame specifically used by MQTT-SN only. It can be sent
// to any MQTT-SN server/gateway that has a server side
// topic registration that matches the topicId supplied.
//
// This is basically a session less publish
//---------------------------------------------------------------------------------------------------
bool packMQTT_SNPublish(short topicId, bool retain,  char* message, uint8_t messageLen){
  byte flag = 0x60; // QoS == 3
  if(retain) {
    flag = flag | 0x10; // Set the Retain flag ( Bit:4)
  }
  uint8_t len = messageLen + MQTT_SN_PUBLISH_HEADER_SIZE;
  if(len > PACKET_LENGTH){
    return false;
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

  bool loop = true;
  while(loop){
  // Send Packet via Radio Head
  #ifdef DEBUG
    Serial.print("Sending to Gateway ");
    Serial.println(LoRaGateway);
  #endif
    if(!manager.sendtoWait(packet, len, LoRaGateway)){
      #ifdef DEBUG
        Serial.println("Failed to send/ No Ack");
      #endif
      currentPower++;
      if(currentPower > MAX_POWER){
        currentPower = MAX_POWER;
        loop = true; // We have failed so just drop out
      }
      else{
        radio.setTxPower(currentPower, powerFlag);
      }
    }
    else{
      #ifdef DEBUG
        Serial.print("Sent and acked, CurrentPower:");
        Serial.println(currentPower);
      #endif
      return true;
    }
  }
  return false;
}

//---------------------------------------------------------------------------------------------------
// Loop through and average out the readings
long readUltrasonicSensor(){
  long durationAve = 0;
  int counter = 0;
  for(int x=0;x<READINGS;x++){
    digitalWrite(TRIGGER_PIN, LOW);
    delayMicroseconds(2);
    digitalWrite(TRIGGER_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIGGER_PIN, LOW);
    long duration = pulseIn(ECHO_PIN, HIGH);
    if(duration > 0 && duration < 380000){
      durationAve += duration;
      counter++;
    }
    delay(100); // Sleep 100ms between readings
  }
  if(counter != 0){
    return durationAve/counter;
  }
  return 0;
}

//---------------------------------------------------------------------------------------------------
// calculate the data and send the event
//---------------------------------------------------------------------------------------------------
void publishEvent() {
  //-----------------------------------------------------------
  // Read the current ultrasonic depth
  //-----------------------------------------------------------
  unsigned long timings = readUltrasonicSensor();
  unsigned long cm = timings / 2; // Its an echo so we don't need round trip
  cm /= 26;              // now convert to cm

  //-----------------------------------------------------------
  // Now read the Analogue 0 input for the current battery charge
  //-----------------------------------------------------------
  float batteryReading = getCurrentVoltage();
  String send = "{\"time\":" + String(timings)+",\"cm\":"+ cm+",\"VBat\":"+batteryReading+",\"loop\":"+ String(loopCounter) +"}";
  #ifdef DEBUG
    Serial.println(send);
  #endif
  sendReading(topicNumber, send);
}

//---------------------------------------------------------------------------------------------------
// same as adafruit, there are 2 * 100K ohm resisters providing a 1/2 voltage divider
//---------------------------------------------------------------------------------------------------

float getCurrentVoltage(int pin){
  float analogReading = analogRead(pin);
  analogReading *= 2;
  analogReading *= 3.3; // this is the reference voltage
  analogReading /= 1024; // Convert to volts
  return analogReading;
}

float getCurrentVoltage(){
  float analogReading = getCurrentVoltage(BATTERY_LEVEL_PIN);
  if(analogReading == 0.0){
    analogReading = getCurrentVoltage(BATTERY_BUILT_IN_PIN);
  }
  return analogReading;
}

//---------------------------------------------------------------------------------------------------
// Send reading via MQTT
//---------------------------------------------------------------------------------------------------
void sendReading(uint16_t topic, String value){
  size_t len = value.length()+1;
  char* p = (char*) malloc(len);
  value.toCharArray(p, len);  
  packMQTT_SNPublish(topic, true, p, len);
  free(p);  
}

//---------------------------------------------------------------------------------------------------
// SETUP Digital pins for Ultrasonic
//---------------------------------------------------------------------------------------------------
void setupUltrasonic(){
  pinMode(TRIGGER_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  digitalWrite(TRIGGER_PIN, LOW); 
}

//---------------------------------------------------------------------------------------------------
// Set the reference pin low then high and test the resultant test pin, it should be high if wired
//---------------------------------------------------------------------------------------------------
boolean checkBoardNumber(){
  pinMode(REFERENCE_PIN, OUTPUT);
  pinMode(TEST_PIN, INPUT);
  
  digitalWrite(REFERENCE_PIN, HIGH);  
  delay(100);
  return digitalRead(TEST_PIN) == HIGH;
}

//---------------------------------------------------------------------------------------------------
// WIRELESS API
//
// NOTE: This will vary between hardware modules
//---------------------------------------------------------------------------------------------------
void setupWireless(){
  pinMode(RFM_RST, OUTPUT);
  digitalWrite(RFM_RST, HIGH);
  delay(100);

  // manual reset
  digitalWrite(RFM_RST, LOW);
  delay(10);
  digitalWrite(RFM_RST, HIGH);
  delay(10);

  if (!manager.init()) {
    while (1);
  }

  if (!radio.setFrequency(RADIO_FREQ)) {
    while (1);
  }
  radio.setTxPower(currentPower, powerFlag);  // range from 14-20 for power, 2nd arg must be true for 69HCW
  #ifdef DEBUG
    Serial.print("RFM radio @");  Serial.print((int)RADIO_FREQ);  Serial.println(" MHz");
  #endif
}

//---------------------------------------------------------------------------------------------------
// Delay the loop, either use the watch dog sleep that puts the chip to sleep or the simple delay
// Using the Watchdog.sleep disables the USB serial port and as such makes it difficult to 
// communicate with the arduino
//---------------------------------------------------------------------------------------------------
unsigned long delayProcessing(unsigned long delayTime){
  #ifdef DEBUG
    Serial.print("Sleeping:");
    Serial.println(delayTime);
    delay(delayTime);
    return delayTime;
  #else
    return Watchdog.sleep(delayTime);
  #endif
}

//---------------------------------------------------------------------------------------------------
// Arduino entry point
//---------------------------------------------------------------------------------------------------
void setup() {
  #ifdef DEBUG
    while(!Serial){};
    Serial.begin(115200);
  #endif
  if(checkBoardNumber()){
    #ifdef DEBUG
      Serial.println("Detected Board # 0");
    #endif
    topicNumber   =  8;
    deviceAddress = 30;
     longDelay = 15 * minute;
    #ifdef DEBUG
      longDelay = 2000;
    #endif

  }
  else{
    #ifdef DEBUG
      Serial.println("Detected Board # 1");
    #endif
    topicNumber   =  9;
    deviceAddress = 31;
    longDelay = 13 * minute;
    #ifdef DEBUG
      longDelay = 2000;
    #endif
  }
  manager.setThisAddress(deviceAddress);
  
  // Setup the rfm95 chip and then put it to sleep for future use
  setupWireless();
  radio.sleep();
  loopCounter = 0;
}

//---------------------------------------------------------------------------------------------------
// Arduino loop function
//---------------------------------------------------------------------------------------------------
void loop() {
  loopCounter++;
  unsigned long sleepCount = 0;
  while(sleepCount < longDelay){
    sleepCount += delayProcessing(longDelay);
  }

  // if the battery voltage is high enough lets power up 
  // and send the data, else lets go back to sleep
 // if(getCurrentVoltage() >= 3.1){
    setupWireless();
    setupUltrasonic();
    publishEvent();
    radio.sleep();
 // }
}
