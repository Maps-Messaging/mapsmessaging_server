/
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


/***************************************************************************
  Weather Station for the Arduino, supports both the ESP32 and ESP8266
  feathers from Adafruit.

  The  devices used are:

  DME280 : that supplies the Temperature, Humidity and pressure : https://www.adafruit.com/product/2652
  BMP180 : Supplies temperature and pressure only               : https://www.adafruit.com/product/1603
  AM2315 : enclosed external temperature and humidity sensor    : https://www.adafruit.com/product/1293
  DS3231 : supplies a 1Hz pulse that is used to maintain a clock that is used
           as a mechanism to calculate wind speed, rain rates as well as clock
           the MQTT message updates
           This chip also supplies an EEPROM device that we use for configuration
           and for data logging, its a AT24C32 I2C device

         https://www.openimpulse.com/blog/products-page/product-category/ds3231-i2c-precision-clock-at24c32-memory/


  Interrupts are as follows

    Pulse Per Second : PIN 12
    Wind Speed pulse : PIN 13
    Rain measurement : PIN 10
    RFM69 interrupt  : PIN  7 - Used by RadioHead for packaging datagrams

  Written by Matthew Buckton.

  BSD license, all text above must be included in any redistribution
 ***************************************************************************/

//--------------------------------------------------------
// Include the external sensor libraries
//
#include <Wire.h>            // Base hardware access to I2C
#include <Adafruit_Sensor.h> // Base include for all Adafruit sensors
#include <Adafruit_BME280.h> // Temp, Humidity and pressure sensor
#include <Adafruit_AM2315.h> // Temp and humidity sensor
#include <Adafruit_BMP085_U.h> // Pressure, Temp sensor from Bosch via Adafruit
#include <DS3231.h>            // RTC

#include <RH_RF95.h>
#include <RHReliableDatagram.h>

//--------------------------------------------------------
// LoRa configuration
//
#define RFM_CS      8
#define RFM_RST     4
#define RFM_INT     3
#define POWER      20
#define PACKET_LENGTH RH_MAX_MESSAGE_LEN
#define RADIO_FREQ 915.0

//--------------------------------------------------------
// MQTT-SN configuration
#define MQTT_SN_PUBLISH_HEADER_SIZE 7
#define weatherTopicId 40

//--------------------------------------------------------
// Definitions for interrupt pins
//
#define RAIN_INT_PIN  13
#define WIND_INT_PIN  10
#define PULSE_PER_SECOND_PIN 12
#define ANALOG_IN A2

//--------------------------------------------------------
// I2C addresses
//
#define BMP180_I2C_ADDRESS 0x77

//--------------------------------------------------------
//
#define DEBUG          1
#define MINUTE        60

//--------------------------------------------------------
// Wind Speed / Direction and Rain gauge constants
//
#define KM_PER_HOUR_PER_PULSE 2.40    // 1 pulse per second == 2.4 KM/H
#define RAIN_VOLUME_PER_PULSE 0.2794  // millimeters

//--------------------------------------------------------
// Create global variables
//
Adafruit_BME280 bme;                  // Temperature, Humidity and pressure sensor
DS3231 rtc;                           // Real Time Clock
Adafruit_AM2315 am2315;               // Temperature and humidity
Adafruit_BMP085_Unified bmp180 = Adafruit_BMP085_Unified(10085);


//--------------------------------------------------------
// Setup MQTT variables
//
int lastResult;

//--------------------------------------------------------
// Variables for the interrupt handlers
//
volatile int windSpeedInterruptCount;
volatile int rainGaugeInterruptCount;
volatile int pulseCount;

//--------------------------------------------------------
// Loop variables
//
int interruptTest;

//--------------------------------------------------------
// sensor flags
//
bool hasRTC;
bool hasConfig;
bool hasBMP180;
bool hasDME280;
bool hasAM2315;

//--------------------------------------------------------
// Current temp and humidity, need a 2 second delay between each read
//
float currentTemperature;
float currentHumidity;
boolean isTemperature;

//--------------------------------------------------------
// LoRa radio configuration
bool powerFlag = false;
uint8_t LORA_GATEWAY = 1;
uint8_t MY_ADDRESS = 21;
uint8_t packet [PACKET_LENGTH];

RH_RF95 radio(RFM_CS, RFM_INT);
RHReliableDatagram manager(radio, MY_ADDRESS);


//--------------------------------------------------------
// Helper classes for DEBUG
//
void write(String msg) {
#ifdef DEBUG
  Serial.println(msg);
#endif
}

void write(char* msg) {
#ifdef DEBUG
  Serial.println(msg);
#endif
}

void write(int val){
#ifdef DEBUG
  Serial.print("Int:");
  Serial.println(val);
#endif
}

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

  byte flag = 0x60; // QoS == 3 * Bits 5&6, 0b0110 0000
  if(retain) {
    flag = flag | 0x10; // Set the Retain flag ( Bit:4, 0b00010000)
  }
  uint8_t len = messageLen + MQTT_SN_PUBLISH_HEADER_SIZE;
  if(len > PACKET_LENGTH){
    write("Buffer larger than acceptable size");
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
    write("Failed to send/ No Ack");
  }
  else{
    write("Successfully received and Acked");
  }
}

//---------------------------------------------------------------------------------------------------
// WIRELESS API
//
// NOTE: This will vary between hardware modules
//
void setupWireless(){
  write("Resetting RH_95");
  pinMode(RFM_RST, OUTPUT);
  digitalWrite(RFM_RST, HIGH);
  delay(100);

  // manual reset
  digitalWrite(RFM_RST, LOW);
  delay(10);
  digitalWrite(RFM_RST, HIGH);
  delay(10);
  
  if (!manager.init()) {
    write("Radio init failed");
    while (1);
  }
  Serial.println("Radio init OK!");

  if (!radio.setFrequency(RADIO_FREQ)) {
    write("setFrequency failed");
  }

  radio.setTxPower(POWER, powerFlag);  // range from 14-20 for power, 2nd arg must be true for 69HCW

  // The encryption key has to be the same as the one in the server
  uint8_t key[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16};
 // radio.setEncryptionKey(key);

  write("RFM radio @");  write((int)RADIO_FREQ); write(" MHz");
}

// --------------------------------------------------------------------------
// I2C control and setup
//
bool start_i2c() {
  write("Starting I2C device scan");
  //----------------------------------------
  //
  hasDME280 = bme.begin();
  if (hasDME280) {
    write("Detected DME280 sensor, being configured");
    bme.setSampling();
    write("I2C Bus Started, setting BME sampling to default settings");
  }

  //----------------------------------------
  // Check for AM2315
  int counter = 0;
  hasAM2315 = false;
  while(counter < 10 && !hasAM2315){
    delay(200);
    counter++;
    hasAM2315 = am2315.begin();
    if(hasAM2315){
      write("Detected AM2135 sensor, being configured");
      counter = 10;
    }
    else{
      write("Scanning for AM2135 sensor:");
    }
  }

  //----------------------------------------
  // Check for BMP180
  delay(100);
  hasBMP180 = bmp180.begin();
  if(hasBMP180){
    write("Detected BMP180 sensor, being configured");
    delay(100);
    sensors_event_t event;
    bmp180.getEvent(&event);
    write("I2C Bus Started, setting bmp sampling to default settings");
  }

  //----------------------------------------
  //
  write("Starting DS3231 RTC 1Hz clock");
  rtc.setSecond(1);
  rtc.enableOscillator(true, false, 0); // Enable 1Hz
  rtc.enable32kHz(true);
  if(rtc.oscillatorCheck()){
    write("RTC Oscillator working correctly");
    hasRTC = true;
  }
  else{
    write("RTC Oscillator failed to initialise");
    hasRTC = false;
  }

  write("Completed I2C device scan");
  //----------------------------------------
  return true;
}

//--------------------------------------------------------------------------
// Setup Interrupt handlers
//
void rainGaugeInterruptHandler();
void windSpeedInterruptHandler();
void PPSInterruptHandler();
//
// We check the time so we can avoid bounce on the sensor
//
unsigned long rainInterrupt = 0;
unsigned long windInterrupt = 0;

void rainGaugeInterruptHandler(){
  unsigned long now = millis();
  if(rainInterrupt < now){
    rainGaugeInterruptCount++;
    rainInterrupt = now + 1;
  }
}

void windSpeedInterruptHandler(){
  unsigned long now = millis();
  if(rainInterrupt < now){
    windSpeedInterruptCount++;
    windInterrupt = now + 1;
  }
}

void PPSInterruptHandler(){
  pulseCount++;
}

void setupInterrupts(){
  pinMode(WIND_INT_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(WIND_INT_PIN), windSpeedInterruptHandler, FALLING);

  pinMode(RAIN_INT_PIN, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(RAIN_INT_PIN), rainGaugeInterruptHandler, FALLING);

  if(hasRTC){
    write("Using RTC PPS Interrupt for second detection");
    pinMode(PULSE_PER_SECOND_PIN, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(PULSE_PER_SECOND_PIN), PPSInterruptHandler, FALLING);
  }
  else{
    write("Unable to use RTC PPS Interrupt for second detection, using software");
    pinMode(LED_BUILTIN, OUTPUT);
  }
}


//--------------------------------------------------------
// Sensor functions
//
double getTemperature(){
  double temp = 0.0;

  //--------------------------------------------------
  //
  if(hasDME280){
    int count = 0;
    while (temp == 0.0 && count < 5) {
      temp = (float)bme.readTemperature();
      if (temp == 0.0) {
        delay(500);
      }
      count++;
    }
  }

  //--------------------------------------------------
  //
  else if(hasAM2315){
    temp = am2315.readTemperature();
  }

  //--------------------------------------------------
  //
  else if(hasBMP180){
    sensors_event_t event;
    bmp180.getEvent(&event);
    temp = event.temperature;
  }
  return temp;
}

double getPressure(){

  double pressure = 0.0;
  //--------------------------------------------------
  if(hasDME280){
    pressure = bme.readPressure();
  }
  //--------------------------------------------------
  else if (hasBMP180){
    //start a pressure measurement. pressure measurements depend on temperature measurement, you should only start a pressure
    //measurement immediately after a temperature measurement.
    sensors_event_t event;
    bmp180.getEvent(&event);
    pressure = event.pressure;
  }
  return pressure;
}

double getHumidity(){
  double humidity = 0.0;

  //--------------------------------------------------
  if(hasAM2315){
    delay(1000);
    humidity = am2315.readHumidity();
  }
  //--------------------------------------------------
  else if(hasDME280){
    humidity = bme.readHumidity();
  }
  return humidity;
}

//--------------------------------------------------------
// Publish the sensor data
//
void publishWeatherEvent(double wind, double rain) {
  write("+---------------------------+");
  write("| MQTT Weather Publish Data |");
  write("+---------------------------+");
  double temp = currentTemperature;
  double pressure = getPressure();
  double humidity = currentHumidity;

  String webString = "{ \"temperature: \"" + String(temp) + "," +
                     " \"humidity: \""     + String(humidity) + "," +
                     " \"pressure: \""     + String(pressure) + ","+
                     " \"rain: \" "          + rain + ","+
                     " \"wind: \""         + wind + "}";

  size_t len = webString.length()+1;
  char* p = (char*) malloc(len);
  webString.toCharArray(p, len);
  packMQTT_SNPublish(weatherTopicId, true, p, len);
  free(p);
}

int readAnalog(int pin){
  return analogRead(pin);
}

//<editor-fold desc="Main Loop functions">
//--------------------------------------------------------------------------
//
int waitForSomething(){
  write("+----------------------+");
  write("|  sleep for a minute  |");
  write("+----------------------+");

  int checkCounter = pulseCount +3;
  isTemperature = true;
  int secondCount = 0;
  bool on = false;
  while(pulseCount < MINUTE){
    delay(1);
    if(checkCounter < pulseCount){
      checkCounter = pulseCount +3;
      if(isTemperature){
        currentTemperature = getTemperature();
        isTemperature = false;
      }
      else{
        isTemperature = true;
        currentHumidity = getHumidity();
      }
    }
    if(!hasRTC){
      secondCount++;
      if(secondCount > 450 && !on){
      int val = readAnalog(ANALOG_IN);
      if(val != -1){
        if(val > 2400 & val < 2600){
          write("North");
        }
        else if(val > 900 & val < 1020){
          write("North East");
        }
        else if(val > 3500 & val < 3600){
          write("East");
        }
        else if(val > 2000 & val < 2160){
          write("South East");
        }
        else if(val > 4000 & val < 4096){
          write("South");
        }
       else if(val > 2900 & val < 3040){
          write("South West");
        }
        else if(val > 700 & val < 850){
          write("West");
        }
        else if(val > 3100 & val < 3300){
          write("North West");
        }
        else{
          write(val);
        }
      }

        on = true;
        write(LED_BUILTIN);
       // digitalWrite(LED_BUILTIN, HIGH);
      }
      else if( secondCount < 450 && on ){
        on = false;
        //digitalWrite(LED_BUILTIN, LOW);
      }
      if(secondCount == 900){
        pulseCount++;
        secondCount = 0;
      }
    }
  }
}

//--------------------------------------------------------------------------
// Send a weather update to the MQTT server
//
int sendWeatherUpdate(){
  if(pulseCount >= 60){
    int pulses = pulseCount;
    pulseCount=0;

    double rain = rainGaugeInterruptCount;
    rainGaugeInterruptCount = 0;

    double wind = windSpeedInterruptCount;
    windSpeedInterruptCount = 0;

    //
    // Calculate the KM/H and the rain mm
    //
    wind = (wind * KM_PER_HOUR_PER_PULSE)/pulses;
    rain = (rain * RAIN_VOLUME_PER_PULSE)/pulses;

    publishWeatherEvent(wind, rain);
  }
}
//</editor-fold>

//<editor-fold desc="ARDUINO FUNCTIONAL ENTRY POINTS">
//--------------------------------------------------------------------------
// Setup the device for operation
//
void setup() {
  pulseCount = 0;

#ifdef DEBUG
  Serial.begin(115200);
  write("setup() called");
  write("Weather Station Management");
#endif
  write("+------------------------+");
  write("| Setup function entered |");
  write("+------------------------+");

  while (!start_i2c()) {
    write("I2C bus failed to start");
  }

  write("Mapping interrupts");
  setupInterrupts();

  write("Setup LoRa radio");
  setupWireless();

  write("+-------------------------+");
  write("| Setup function complete |");
  write("+-------------------------+");
}

//--------------------------------------------------------------------------
// Main device loop, we simply
//
void loop() {
  waitForSomething();
  sendWeatherUpdate();
}
//</editor-fold>
