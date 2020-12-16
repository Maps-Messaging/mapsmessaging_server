// rf95 demo tx rx.pde
// -*- mode: C++ -*-
// Example sketch showing how to create a simple addressed, reliable messaging client
// with the RH_RF95 class. RH_RF95 class does not provide for addressing or
// reliability, so you should only use RH_RF95  if you do not need the higher
// level messaging abilities.
// It is designed to work with the other example rf95_server.
// Demonstrates the use of AES encryption, setting the frequency and modem 
// configuration

//
// Set up the wireless API
//
#include <SPI.h>
#include <RH_RF95.h>
#include <RHReliableDatagram.h>


/************ Radio Setup ***************/

// Change to 434.0 or other frequency, must match RX's freq!
#define RF95_FREQ 915.0
#define MAX_BUFFER_SIZE 64
#define MQTT_SN_PUBLISH_HEADER_SIZE 7

// change addresses for each client board, any number :)
uint8_t LORA_GATEWAY = 1;
uint8_t MY_ADDRESS = 2;

#define RFM95_CS      8
#define RFM95_INT     7
#define RFM95_RST     4
#define LED           13

// Singleton instance of the radio driver
RH_RF95 rf95(RFM95_CS, RFM95_INT);
RHReliableDatagram manager(rf95, MY_ADDRESS);

// Class to manage message delivery and receipt, using the driver declared above
uint8_t packet [MAX_BUFFER_SIZE];
uint16_t u16TopicID = 10;

void packMQTT_SNPublish(short topicId, bool retain,  char* message, uint8_t messageLen){
  //
  // Init
  //
  
  byte flag = 0x60; // QoS == 3
  if(retain) {
    flag = flag | 0x10; // Set the Retain flag ( Bit:4)
  }
  uint8_t len = messageLen + MQTT_SN_PUBLISH_HEADER_SIZE;
  if(len > MAX_BUFFER_SIZE){
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


void setup()
{
 delay(2000);
  Serial.begin(115200);

  pinMode(LED, OUTPUT);     
  pinMode(RFM95_RST, OUTPUT);
  digitalWrite(RFM95_RST, LOW);

  Serial.println("Feather Addressed RFM95 TX Test!");
  Serial.println();

  // manual reset
  digitalWrite(RFM95_RST, HIGH);
  delay(10);
  digitalWrite(RFM95_RST, LOW);
  delay(10);
  
  if (!manager.init()) {
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
  rf95.setTxPower(20, true);  // range from 14-20 for power, 2nd arg must be true for 95HCW

  pinMode(LED, OUTPUT);

  Serial.print("RFM95 radio @");  Serial.print((int)RF95_FREQ);  Serial.println(" MHz");
}

void loop() {
  char* message = "Hi There this is a simple";
  packMQTT_SNPublish(u16TopicID, false, message, strlen(message));
  Serial.print("Sent message to ");
  Serial.println(u16TopicID);
  delay(10000);
}
