/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.mapsmessaging.network.protocol.impl.cot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import org.junit.jupiter.api.Test;

class CotProtocolFactoryTest {

  @Test
  void advertises_cot_as_tcp_stream_protocol() {
    CotProtocolFactory factory = new CotProtocolFactory();
    assertEquals("CoT", factory.getName());
    assertEquals("tcp", factory.getTransportType());
    assertTrue(factory.matches("cot"));
  }

  @Test
  void is_registered_as_a_protocol_service() throws IOException {
    String serviceName = "META-INF/services/" + ProtocolImplFactory.class.getName();
    Enumeration<URL> services = CotProtocolFactoryTest.class.getClassLoader().getResources(serviceName);
    boolean registered = false;
    while (services.hasMoreElements() && !registered) {
      try (InputStream input = services.nextElement().openStream()) {
        String providers = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        registered = providers.lines().anyMatch(CotProtocolFactory.class.getName()::equals);
      }
    }
    assertTrue(registered);
  }
}
