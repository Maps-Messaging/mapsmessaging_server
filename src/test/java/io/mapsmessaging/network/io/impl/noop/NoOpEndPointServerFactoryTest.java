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
import java.io.IOException;
import org.junit.jupiter.api.Test;

class NoOpEndPointServerFactoryTest {

  @Test
  void instance() throws IOException {
    NoOpEndPointServerFactory factory = new NoOpEndPointServerFactory();
    EndPointURL url = new EndPointURL("local://localhost/");
    AcceptHandler handler = new AcceptHandler() {
      @Override
      public void accept(EndPoint endpoint) {
        // nothing here, just a placeholder
      }
    };
    assertNotNull(factory.instance(url, null, handler, null, null ));
  }

  @Test
  void active() {
    NoOpEndPointServerFactory factory = new NoOpEndPointServerFactory();
    assertTrue(factory.active());
  }

  @Test
  void getName() {
    NoOpEndPointServerFactory factory = new NoOpEndPointServerFactory();
    assertEquals("noop", factory.getName());
  }

  @Test
  void getDescription() {
    NoOpEndPointServerFactory factory = new NoOpEndPointServerFactory();
    assertEquals("Provides a EndPoint object for a loop based protocol", factory.getDescription());
  }
}