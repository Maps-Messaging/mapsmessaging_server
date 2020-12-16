// rf69 demo tx rx.pde
// -*- mode: C++ -*-
// Example sketch showing how to create a simple addressed, reliable messaging client
// with the RH_RF69 class. RH_RF69 class does not provide for addressing or
// reliability, so you should only use RH_RF69  if you do not need the higher
// level messaging abilities.
// It is designed to work with the other example rf69_server.
// Demonstrates the use of AES encryption, setting the frequency and modem 
// configuration

//
// Set up the wireless API
//
#include <SPI.h>
#include <RH_RF95.h>
#include <RHReliableDatagram.h>

//
// MQTT-SN Client from https://github.com/bngesp/MQTTSN-over-LoRA
//
#include "mqttsn-messages.h"


/************ Radio Setup ***************/

// Change to 434.0 or other frequency, must match RX's freq!
#define RF95_FREQ 915.0


// change addresses for each client board, any number :)
#define MY_ADDRESS  20

// for Feather32u4 RFM9x
#define RFM95_CS 8
#define RFM95_RST 4
#define RFM95_INT 7
#define LED 13

// Singleton instance of the radio driver
RH_RF95 rf95(RFM95_CS, RFM95_INT);

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf95_manager(rf95, MY_ADDRESS);
MQTTSN mqttsn(&rf95_manager);


#define TOPIC "testII"

uint16_t u16TopicID;
bool isLow = true;



void setup() 
{
  Serial.begin(115200);

  pinMode(LED, OUTPUT);     

  Serial.println("Feather Addressed RFM95 TX Test!");
  Serial.println();

   pinMode(RFM95_RST, OUTPUT);
   digitalWrite(RFM95_RST, HIGH);
   delay(100);

   // manual reset
   digitalWrite(RFM95_RST, LOW);
   delay(10);
   digitalWrite(RFM95_RST, HIGH);
   delay(10);

  if (!rf95_manager.init()) {
    Serial.println("RFM95 radio init failed");
    while (1);
  }
  Serial.println("RFM95 radio init OK!");
  // Defaults after init are 434.0MHz, modulation GFSK_Rb250Fd250, +13dbM (for low power module)
  // No encryption
  if (!rf95.setFrequency(RF95_FREQ)) {
    Serial.println("setFrequency failed");
  }

  // If you are using a high power RF95 eg RFM95HW, you *must* set a Tx power with the
  // ishighpowermodule flag set like this:
  rf95.setTxPower(20, false);  // range from 14-20 for power, 2nd arg must be true for 95HCW 
  pinMode(LED, OUTPUT);
  Serial.print("RFM95 radio @");  Serial.print((int)RF95_FREQ);  Serial.println(" MHz");
}


uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];

void loop() {
  uint8_t index;
  while(mqttsn.wait_for_response()){
    delay(1);
  }
  if (!mqttsn.connected()) {
    Serial.println("Send Connect");
    mqttsn.connect(0, 10, "arduinoII");
    return;
  }
  else{
    Serial.println("Connected");
  }

  u16TopicID = mqttsn.find_topic_id(TOPIC, &index);
  if (u16TopicID == 0xffff) {
    Serial.println("Register Topic");
    mqttsn.register_topic(TOPIC);
    Serial.print("Registered Topic::");
    Serial.println(u16TopicID);
    return;
  }

  char str[50] = "Hello MQTT-SN World! from Arduino II";
  mqttsn.publish(32, u16TopicID, str, strlen(str));
  delay(1000);
}

void MQTTSN_serial_send(uint8_t *message_buffer, int length) {
}

void MQTTSN_publish_handler(const msg_publish *msg) {
}

void MQTTSN_gwinfo_handler(const msg_gwinfo *msg) {
}

void CheckSerial() {
  
}
