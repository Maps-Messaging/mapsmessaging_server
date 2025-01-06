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

// ---------------------------------------------------------------------------------------------------//
// This code simply accepts a socket and writes whatever it receives from the socket to serial and    //
// writes whatever it reads from the serial to the socket. This enables a simple serial protocol test //
// ---------------------------------------------------------------------------------------------------//

#include <ESP8266WiFi.h>
 
const char* ssid     = ""; // Update ssid
const char* password = ""; // Update password

WiFiServer wifiServer(8080); 
 
void setup() {  
  Serial.begin(115200);
  WiFi.begin(ssid, password);  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
  wifiServer.begin();
  delay(1000);
}
 
void loop() {
  WiFiClient client = wifiServer.available(); 
  if (client) {
    while(client){
      while(client.available()){
        char c = client.read();
        Serial.write(c);
      }

      while(Serial.available()){
        char c = Serial.read();
        client.write(c);
      }
    }
    ESP.restart();
  }
  delay(100);
}
