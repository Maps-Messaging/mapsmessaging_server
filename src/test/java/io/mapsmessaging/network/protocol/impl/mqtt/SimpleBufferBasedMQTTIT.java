/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.protocol.impl.mqtt;

import io.mapsmessaging.test.SimpleBufferBasedTest;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimpleBufferBasedMQTTIT extends SimpleBufferBasedTest {

  @Test
  @DisplayName("Send MQTT frame single character at a time")
  void simpleCharByCharTest() throws IOException, URISyntaxException {
    simpleByteWriteTest(1);
  }

  @Test
  @DisplayName("Send MQTT frame single character at a time")
  void simple2By2Test() throws IOException, URISyntaxException {
    simpleByteWriteTest(2);
  }

  @Test
  @DisplayName("Send MQTT frame single character at a time")
  void simple5By5Test() throws IOException, URISyntaxException {
    simpleByteWriteTest(5);
  }

  private void simpleByteWriteTest(int size) throws IOException, URISyntaxException {
    simpleByteWrite("/mqtt_3_1_1.txt", 0x20, size, "localhost", 1888, 1);
  }

  @Test
  @DisplayName("Delay subscriber by 1ms to force flow control back to the server")
  void slowSubscriberWith1msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest("/mqtt_3_1_1.txt", "localhost", 1888, -1, 0x20, 1);
  }

  @Test
  @DisplayName("Delay subscriber by 10ms to force flow control back to the server")
  void slowSubscriberWith10msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest("/mqtt_3_1_1.txt", "localhost", 1888, -1, 0x20, 10);
  }

  @Test
  @DisplayName("Delay subscriber by 50ms to force flow control back to the server")
  void slowSubscriberWith50msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest("/mqtt_3_1_1.txt", "localhost", 1888, -1, 0x20, 50);
  }

}