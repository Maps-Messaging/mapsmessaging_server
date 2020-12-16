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
#include <RH_RF69.h>
#include <RHReliableDatagram.h>

//
// MQTT-SN Client from https://github.com/bngesp/MQTTSN-over-LoRA
//
#include "mqttsn-messages.h"


/************ Radio Setup ***************/

// Change to 434.0 or other frequency, must match RX's freq!
#define RF69_FREQ 915.0


// change addresses for each client board, any number :)
#define LORA_GATEWAY 1
#define MY_ADDRESS   2


#define RFM69_CS      8
#define RFM69_INT     7
#define RFM69_RST     4
#define LED           13

// Singleton instance of the radio driver
RH_RF69 rf69(RFM69_CS, RFM69_INT);

// Class to manage message delivery and receipt, using the driver declared above
RHReliableDatagram rf69_manager(rf69, MY_ADDRESS);
MQTTSN mqttsn(&rf69_manager);


#define TOPIC "test"

uint16_t u16TopicID;
bool isLow = true;

const int initialCountdown = 100;
int countdown = 0;


void setup() 
{
  delay(2000);
  Serial.begin(115200);

  pinMode(LED, OUTPUT);     
  pinMode(RFM69_RST, OUTPUT);
  digitalWrite(RFM69_RST, LOW);

  Serial.println("Feather Addressed RFM69 TX Test!");
  Serial.println();

  // manual reset
  digitalWrite(RFM69_RST, HIGH);
  delay(10);
  digitalWrite(RFM69_RST, LOW);
  delay(10);
  
  if (!rf69_manager.init()) {
    Serial.println("RFM69 radio init failed");
    while (1);
  }
  Serial.println("RFM69 radio init OK!");
  // Defaults after init are 434.0MHz, modulation GFSK_Rb250Fd250, +13dbM (for low power module)
  // No encryption
  if (!rf69.setFrequency(RF69_FREQ)) {
    Serial.println("setFrequency failed");
  }

  // If you are using a high power RF69 eg RFM69HW, you *must* set a Tx power with the
  // ishighpowermodule flag set like this:
  rf69.setTxPower(20, true);  // range from 14-20 for power, 2nd arg must be true for 69HCW

  // The encryption key has to be the same as the one in the server
  uint8_t key[] = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                    0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16};
  rf69.setEncryptionKey(key);
  
  pinMode(LED, OUTPUT);

  Serial.print("RFM69 radio @");  Serial.print((int)RF69_FREQ);  Serial.println(" MHz");
}


uint8_t buf[RH_RF69_MAX_MESSAGE_LEN];

void loop() {
  uint8_t index;
  while(mqttsn.wait_for_response()){
    Serial.println("Waiting");
    delay(100);
  }
  
  if (!mqttsn.connected()) {
    Serial.println("Send Connect");
    mqttsn.connect(0, 10, "arduino");
    return;
  }

  u16TopicID = mqttsn.find_topic_id(TOPIC, &index);
  if (u16TopicID == 0xffff) {
    Serial.println("Register Topic");
    mqttsn.register_topic(TOPIC);
    Serial.print("Registered Topic::");
    Serial.println(u16TopicID);
    return;
  }

  if(countdown <= 0){
    char str[50] = "Hello MQTT-SN World!";
    mqttsn.publish(0, u16TopicID, str, strlen(str));
    Serial.println("Sent pkt");
    countdown = initialCountdown;
    
  }
  countdown--;
  delay(100);
}

void MQTTSN_serial_send(uint8_t *message_buffer, int length) {
}

void MQTTSN_publish_handler(const msg_publish *msg) {
}

void MQTTSN_gwinfo_handler(const msg_gwinfo *msg) {
}

void CheckSerial() {
  
}
