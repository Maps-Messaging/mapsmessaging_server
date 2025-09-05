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
import io.mapsmessaging.network.io.AcceptHandler;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class NoOpEndPointConnectionFactoryTest {

  @Test
  void connect() throws IOException {
    NoOpEndPointConnectionFactory factory = new NoOpEndPointConnectionFactory();
    EndPointURL url = new EndPointURL("local://localhost/");
    AtomicBoolean connected = new AtomicBoolean(false);
    EndPointConnectedCallback connectedCallback = endpoint -> connected.set(true);
    AcceptHandler handler = endpoint -> {};
    NoOpEndPointServer server = new NoOpEndPointServer(handler, url, null);
    EndPoint endPoint = factory.connect(url, null, connectedCallback, server, new ArrayList<>());
    assertNotNull(endPoint);
    assertTrue(endPoint instanceof NoOpEndPoint);
    assertTrue(connected.get());
  }

  @Test
  void getName() {
    NoOpEndPointConnectionFactory factory = new NoOpEndPointConnectionFactory();
    assertEquals("noop", factory.getName());
  }

  @Test
  void getDescription() {
    NoOpEndPointConnectionFactory factory = new NoOpEndPointConnectionFactory();
    assertEquals("Provides a EndPoint object for a loop based protocol", factory.getDescription());
  }
}