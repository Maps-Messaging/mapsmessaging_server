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

package io.mapsmessaging.network.io.impl.noop;

import static org.junit.jupiter.api.Assertions.*;

import io.mapsmessaging.network.EndPointURL;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class NoOpEndPointTest {
  private EndPointURL url = new EndPointURL("loop://localhost/");

  private NoOpEndPoint create(){
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    return new NoOpEndPoint(0, noOpEndPointServer, new ArrayList<>());
  }

  @Test
  void close() {
    assertDoesNotThrow(()->create().close());
  }

  @Test
  void getProtocol() {
    assertDoesNotThrow(()->create().getProtocol());
    assertEquals("NoOp",create().getProtocol() );
  }

  @Test
  void sendPacket() throws IOException {
    assertEquals(0,create().sendPacket(null) );
  }

  @Test
  void readPacket() throws IOException {
    assertEquals(0,create().readPacket(null) );
  }

  @Test
  void register() throws IOException {
    assertNull(create().register(0, null));
  }

  @Test
  void deregister() throws ClosedChannelException {
    assertNull(create().deregister(0));
  }

  @Test
  void getAuthenticationConfig() {
    assertNull(create().getAuthenticationConfig());
  }

  @Test
  void getName() {
    assertDoesNotThrow(()->create().getProtocol());
    assertEquals("NoOp",create().getName() );

  }

  @Test
  void createLogger() {
  }
}