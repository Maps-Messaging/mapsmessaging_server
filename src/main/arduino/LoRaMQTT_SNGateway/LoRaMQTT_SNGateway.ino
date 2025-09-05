//
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
//
// For different hardware, you will need to change
//    a) The RH_radio include to match your wireless device
//    b) The #defines for the frequency and pins for interrupts and reset
//    c) Any reference to the radio
//
// Please Note: This was developed on a Adafruit Feather 32u4 Radio (RFM69HCW) 900MHz,
//              the Adafruit Feather 32u4 Radio (RFM69HCW) 900MHz,
//              the Adafruit Feather M0 Radio with RFM69 Packet Radio, 
//              and the Adafruit Feather M0 Radio with RFM95 Packet Radio, 
//              see links below
//


//---------------------------------------------------------------------------------------------------
// Define which Radio chip set to use
//
#define RH_69 1
//#define RH_95 1
//
//---------------------------------------------------------------------------------------------------

#include <Arduino.h>
#include <SPI.h>

#if defined( RH_69 )
#include <RH_RF69.h>
//----------------------------
// Please set for your hardware
// for Adafruit FeatherM0 RFM69
// https://learn.adafruit.com/adafruit-feather-m0-radio-with-rfm69-packet-radio
//----------------------------
#define RFM_CS      8
#define RFM_INT     3
#define RFM_RST     4
#define LED           13
#define PACKET_LENGTH RH_RF69_MAX_MESSAGE_LEN
#elif defined( RH_95 )
#include <RH_RF95.h>
//----------------------------
// Please set for your hardware
// for Adafruit Feather32u4 RFM9x
// https://learn.adafruit.com/adafruit-feather-32u4-radio-with-lora-radio-module/overview
//----------------------------
#define RFM_CS 8
#define RFM_INT 3
#define RFM_RST 4
#define LED 13
#define PACKET_LENGTH RH_RF95_MAX_PAYLOAD_LEN
//----------------------------
#else
//----------------------------
// If using different hardware please refer to the
// RadioHead libraries and your hardware config
//----------------------------
#error Need to define the Radio Hardware to use
#endif

//----------------------------
// Include the RadioHead reliable datagram
//----------------------------
#include <RHReliableDatagram.h>

//---------------------------------------------------------------------------------------------------
// #DEFINE segment
//
// IMPORTANT : SET YOUR FREQUENCY FOR YOUR RX
//
// Change to 434.0 or other frequency, must match RX's freq!
#define RADIO_FREQ 915.0

//
// Define the different implementations Reset and Init pins
//
// See https://learn.adafruit.com/adafruit-rfm69hcw-and-rfm96-rfm95-rfm98-lora-packet-padio-breakouts
// or https://learn.adafruit.com/adafruit-feather-m0-radio-with-rfm69-packet-radio
// for examples
//

//---------------------------------------------------------------------------------------------------
//
// MQTT-SN Gateway commands
//

// Responses
const byte FAILURE = 0x70; // Operation was unsuccessful
const byte SUCCESS = 0x71; // Operation was successful

// Commands
const byte DATA    = 0x12; // Send the data to the supplied address
const byte CONFIG  = 0x13; // Set the Address, Power and encryption key to use
const byte RESET   = 0x14; // Hardware reset, use with caution, it may bounce the com port on windows OS
const byte START   = 0x15; // Starts transmitting and receiving on the radio if previously stopped
const byte STOP    = 0x16; // Stops transmitting and receiving on the radio if previously started
const byte VERSION = 0x17; // Return the current version of this software
const byte PING    = 0x18; // Something to send when not sending anything
const byte LOG     = 0x19; // Send a log message to console

// Current version is 0.2
uint8_t version = 0x02; // Hex value, first 4 bits == major, 4 lower bits = minor


//
// Framing characters
//
const byte START_FRAME = 0x2;
const byte END_FRAME   = 0x3;
//---------------------------------------------------------------------------------------------------
//
// STATE variables
//
bool hasStarted = false;
bool LED_ON = false;
uint16_t loopCounter = 0;
uint16_t pingCounter = 0;

//---------------------------------------------------------------------------------------------------
//
// Wireless configuration, this should be set by the server
//

// The address that the radio should listen on, this is configured by the server
// and any value here will be overridden by the CONFIG command
int address = 1;

//
// The power that the radio should use
// See https://lora-developers.semtech.com/library/tech-papers-and-guides/the-book/packet-size-considerations/
// NOTE: This is overridden by the CONFIG command from the server
int power = 20;

// The radio chip offers encryption, the RF95 does not, so this only is relevant for the radio
// The encryption key has to be the same as the one in the clients
// NOTE: This is overridden by the CONFIG command from the server
uint8_t key[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};


//---------------------------------------------------------------------------------------------------
//
// In Out Buffers to use, INFO: The max LoRa packet size is 255 bytes
//
uint8_t rf_buffer [PACKET_LENGTH];


//---------------------------------------------------------------------------------------------------
// Radio objects and setup
//
// NOTE::: Change the below to match your wireless hardware
// Singleton instance of the radio driver
#ifdef RH_69
RH_RF69 radio(RFM_CS, RFM_INT);
#endif

#ifdef RH_95
RH_RF95 radio(RFM_CS, RFM_INT);
#endif
//
// Use the RadioHead Reliable Datagram API for the hardware abstraction layer
//
RHReliableDatagram *reliableDatagram;

// Increasing this value slows the blinking on idle traffic
//
const uint16_t TOGGLE_LOOP_COUNT = 60000;
const uint16_t PING_COUNT = 2;

//---------------------------------------------------------------------------------------------------
// WIRELESS API
//
// NOTE: This will vary between hardware modules
//
void setupWireless(){
  pinMode(RFM_RST, OUTPUT);
#ifdef RH_69
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
  digitalWrite(RFM_RST, HIGH);
  delay(100);

  // manual reset
  digitalWrite(RFM_RST, LOW);
  delay(10);
  digitalWrite(RFM_RST, HIGH);
  delay(10);
#endif
}

void setTxPower(){
  #ifdef RH_95
    radio.setTxPower(power, false);
  #endif
  #ifdef RH_69
    radio.setTxPower(power, true);
  #endif
}

void setKey(){
#ifdef RF_69
  radio.setEncryptionKey(key);
#endif
}

void startWireless(){
  reliableDatagram = new RHReliableDatagram(radio, address);
  while (!reliableDatagram->init()) {
    writeSerialCommand(LOG, "INIT FAILED");
    blink(LED, 100, 100); // Blink LED at a rate of 10 / second to indicate that the Reliable Datagram failed to init
  }

  while (!radio.setFrequency(RADIO_FREQ)) {
    writeSerialCommand(LOG, "FREQ FAILED");
    blink(LED, 500, 100); // Blink LED at a rate of 2 / second to indicate frequency setting failed
  }
  setTxPower();
  setKey();
  // At this point the wireless should be configured and ready to use
}

//
// This is the function that reads from the wireless chip and writes to the serial device in a DATA frame
//
void readWireless(){
  if(hasStarted){
    uint8_t len = PACKET_LENGTH;
    uint8_t from=0;
    if (reliableDatagram->recvfromAck(rf_buffer, &len, &from)){
      writeSerialData(from, radio.lastRssi(), len, rf_buffer );
      toggleLED();
      pingCounter = 0;
    }
  }
}

bool writeWireless(uint8_t to, uint8_t len, uint8_t* data) {
  if(hasStarted) {
    return reliableDatagram->sendtoWait(data, len, to);
  }
  return false;
}

//---------------------------------------------------------------------------------------------------
//
// Wait for a serial connection, no point in starting anything until we have a connection
//
void setupSerial(){
  while(!Serial){
    blink(LED, 250,4); // Blink 4 times a second to indicate we are waiting for a serial connection
  }
  Serial.begin(115200);
}

//
// Send a DATA frame to the server for processing
//
void writeSerialData(uint8_t clientID, byte rssi, uint8_t len, uint8_t* data){
  Serial.write(START_FRAME);
  Serial.write(DATA);
  Serial.write(len+2);
  Serial.write(clientID);
  Serial.write(rssi);
  Serial.write(data, len);
  Serial.write(END_FRAME);
  pingCounter = 0;
}

void writeSerialCommand(uint8_t command, char* data){
  uint8_t len = strlen(data);
  Serial.write(START_FRAME);
  Serial.write(command);
  Serial.write(len);
  if(len > 0){
    Serial.write(data, len);
  }
  Serial.write(END_FRAME);
}

void writeSerialCommand(uint8_t command, uint8_t len, uint8_t* data){
  Serial.write(START_FRAME);
  Serial.write(command);
  Serial.write(len);
  if(len > 0){
    Serial.write(data, len);
  }
  Serial.write(END_FRAME);
}

//------------------------------------------------------
// We received a DATA frame to send over the radio
bool processData(uint8_t len){
  uint8_t to = Serial.read(); // Address to send it to
  for(int x=0;x<len;x++){ // Load the data
    rf_buffer[x] = Serial.read();
  }
  // Now send it and return the state to the server
  return writeWireless(to, len, rf_buffer);
}

//------------------------------------------------------
// Read and set the configuration parameters
bool processConfig(uint8_t len){
  if(len == 18){
    address = Serial.read();
    power = Serial.read();
    for(int x=0;x<16;x++){
      key[x] = Serial.read();
    }
    return true;
  }
  return false;
}

//------------------------------------------------------
// Start the radio
bool processStart(uint8_t len){
  if(!hasStarted){
    startWireless();
    hasStarted = true;
    return true;
  }
  return false;
}

//------------------------------------------------------
// Stop the radio
bool processStop(uint8_t len){
  if(hasStarted){
    hasStarted = false;
    delete reliableDatagram;
    return true;
  }
  return false;
}

//------------------------------------------------------
// Process serial commands

void readSerial(){
  if(Serial.available() > 0) {
    while(Serial.available() > 0 && Serial.read() != START_FRAME){} // Eat all the chars till we get a START_FRAME
    if(Serial.available() <=0 ) return;

    uint8_t command = Serial.read();
    uint8_t len = Serial.read(); // The length of the packer
    
    bool result = false;
    switch(command){
      case DATA:
        result = processData(len);
        break;

      case CONFIG:
        result = processConfig(len);
        break;

      case START:
        result = processStart(len);
        break;

      case STOP:
        result = processStop(len);
        break;

      case PING:
        for(int x=0;x<len;x++){
          Serial.read(); // Ignore the payload for now
        }
        return;  // This is a response to US sending a ping

      case VERSION:
        writeSerialCommand(VERSION, 1, &version); // This is the response
        for(int x=0;x<len;x++){
          Serial.read(); // Ignore the payload for now
        }
        return;

      case RESET:
        result = false;
        for(int x=0;x<len;x++){
          Serial.read(); // Ignore the payload for now
        }
        break;

      default:
        while(Serial.available() > 0 && Serial.read() != END_FRAME){}
        result = false;
    }

    if(result){
      writeSerialCommand(SUCCESS, 1, &command);
    }
    else{
      writeSerialCommand(FAILURE, 1, &command);
    }
  }
}


//---------------------------------------------------------------------------------------------------
// MISC Functions
// Not used for serial or wireless
//
void blink(byte PIN, byte DELAY_MS, byte loops) {
  for (byte i=0; i<loops; i++)  {
    digitalWrite(PIN,HIGH);
    delay(DELAY_MS);
    digitalWrite(PIN,LOW);
    delay(DELAY_MS);
  }
}

//---------------------------------------------------------------------------------------------------
// Used to indicate Wireless activity
//
void toggleLED(){
  loopCounter = 0;  
  if(LED_ON){
    LED_ON = false;
    digitalWrite(LED,HIGH);    
  }
  else{
    LED_ON = true;
    digitalWrite(LED,LOW);        
  }
}

//
// Blinks slowly to indicate activity if NO wireless traffic
//
void houseWork(){
  loopCounter++;
  if(loopCounter == TOGGLE_LOOP_COUNT){
    loopCounter =0;
    pingCounter++;
    if(pingCounter == PING_COUNT){
      pingCounter =0;
      uint8_t st = 0;
      if(hasStarted){
        st = 1;
      }
      toggleLED();
      writeSerialCommand(PING, 1, &st); // Indicate current state
    }
  }
}

//---------------------------------------------------------------------------------------------------
// Setup the initial state and configure Serial for reading and Wireless for initial config load
//
void setup() {
  pinMode(LED, OUTPUT);     
  setupSerial();
  setupWireless();
}

//---------------------------------------------------------------------------------------------------
// Main LOOP:
//
// checks wireless, if data read it and, depending on state pack and write to serial
// reads from serial, if data then process it
// then any house keeping that needs to be done
void loop() {
  readWireless();
  readSerial();
  houseWork();
}
