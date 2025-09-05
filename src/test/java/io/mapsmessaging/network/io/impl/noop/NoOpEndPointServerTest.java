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
import org.junit.jupiter.api.Test;

class NoOpEndPointServerTest {
  private EndPointURL url = new EndPointURL("loop://localhost/");

  @Test
  void register()  {
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    assertDoesNotThrow(noOpEndPointServer::register);
  }

  @Test
  void deregister() {
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    assertDoesNotThrow(noOpEndPointServer::deregister);
  }

  @Test
  void start() {
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    assertDoesNotThrow(noOpEndPointServer::start);

  }

  @Test
  void createLogger() {
  }

  @Test
  void close() {
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    assertDoesNotThrow(noOpEndPointServer::close);
  }

  @Test
  void selected() {
    NoOpEndPointServer noOpEndPointServer = new NoOpEndPointServer(null, url, null);
    assertDoesNotThrow(()-> {
      noOpEndPointServer.selected(null, null, 0);
    });
  }
}