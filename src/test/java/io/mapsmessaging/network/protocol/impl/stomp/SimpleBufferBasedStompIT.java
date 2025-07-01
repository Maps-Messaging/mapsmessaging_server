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

package io.mapsmessaging.network.protocol.impl.stomp;

import io.mapsmessaging.test.SimpleBufferBasedTest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SimpleBufferBasedStompIT extends SimpleBufferBasedTest {

  private final int TOTAL_FRAMES = 101;

  @Test
  @Timeout(value = 600, unit = TimeUnit.SECONDS)
  @DisplayName("Send Stomp frame single character at a time")
  void simpleCharByCharTest() throws IOException,  URISyntaxException {
    simpleByteWriteTest(1);
  }


  @Test
  @DisplayName("Send Stomp frame single character at a time with no delay")
  void simpleCharByCharFastTest() throws IOException,  URISyntaxException {
    simpleByteWrite("/input.txt", 0x0, 1, "localhost", 8674, TOTAL_FRAMES, null, true);

  }

  @Test
  @DisplayName("Send Stomp frame 10 characters at a time")
  void simple10BlockTest() throws IOException,  URISyntaxException {
    simpleByteWriteTest(10);
  }

  @Test
  @DisplayName("Send Stomp frame 100 characters at a time")
  void simple100BlockTest() throws IOException, URISyntaxException {
    simpleByteWriteTest(100);
  }

  @Test
  @DisplayName("Send Stomp frame 1K characters at a time")
  void simple1KBlockTest() throws IOException, URISyntaxException {
    simpleByteWriteTest(1024);
  }

  @Test
  @DisplayName("Send Stomp frame 10K characters at a time")
  void simple10KBlockTest() throws IOException, URISyntaxException {
    simpleByteWriteTest(10240);
  }

  @Test
  @DisplayName("Delay subscriber by 1ms to force flow control back to the server")
  void slowSubscriberWith1msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest(1);
  }

  @Test
  @DisplayName("Delay subscriber by 10ms to force flow control back to the server")
  void slowSubscriberWith10msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest(10);
  }

  @Test
  @DisplayName("Delay subscriber by 50ms to force flow control back to the server")
  void slowSubscriberWith50msDelay() throws IOException, URISyntaxException {
    slowSubscriberTest(50);
  }

  @Test
  @DisplayName("Send Stomp frames but delay the framing bytes")
  void frameCharacterTest() throws IOException, URISyntaxException {
    int[] frames = {0x0, 0xA};
    super.frameCharacter("/input.txt", "localhost", 8674, frames, TOTAL_FRAMES);
  }

  private void simpleByteWriteTest(int size) throws IOException, URISyntaxException {
    simpleByteWrite("/input.txt", 0x0, size, "localhost", 8674, TOTAL_FRAMES);
  }

  private void slowSubscriberTest(int delay)throws IOException, URISyntaxException {
    super.slowSubscriberTest("/subscribeLargeEvents.txt", "localhost", 8674, 72, 0x0, delay);
  }

}
