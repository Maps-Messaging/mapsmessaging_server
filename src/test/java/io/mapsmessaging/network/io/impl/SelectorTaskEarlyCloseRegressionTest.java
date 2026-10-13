/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.io.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import org.junit.jupiter.api.Test;

class SelectorTaskEarlyCloseRegressionTest {

  // AMQP 1.0 Part 2 section 2.2: rejection can precede the first selector registration.
  @Test
  void close_before_registration_is_safe_and_idempotent() {
    SelectorCallback callback = mock(SelectorCallback.class);
    when(callback.getEndPoint()).thenReturn(mock(EndPoint.class));
    EndPointConfigDTO config = mock(EndPointConfigDTO.class);
    when(config.getServerReadBufferSize()).thenReturn(1024L);
    when(config.getServerWriteBufferSize()).thenReturn(1024L);
    SelectorTask task = new SelectorTask(callback, config);

    assertDoesNotThrow(task::close);
    assertFalse(task.isOpen());
    assertDoesNotThrow(task::close);
  }
  @Test
  void close_cancels_an_existing_selection_key() throws Exception {
    SelectorCallback callback = mock(SelectorCallback.class);
    EndPoint endpoint = mock(EndPoint.class);
    when(callback.getEndPoint()).thenReturn(endpoint);
    EndPointConfigDTO config = mock(EndPointConfigDTO.class);
    when(config.getServerReadBufferSize()).thenReturn(1024L);
    when(config.getServerWriteBufferSize()).thenReturn(1024L);
    SelectorTask task = new SelectorTask(callback, config);
    var key = mock(java.nio.channels.SelectionKey.class);
    var registered = new java.util.concurrent.FutureTask<java.nio.channels.SelectionKey>(() -> key);
    registered.run();
    when(endpoint.register(java.nio.channels.SelectionKey.OP_READ, task)).thenReturn(registered);
    task.register(java.nio.channels.SelectionKey.OP_READ);

    task.close();

    org.mockito.Mockito.verify(key).cancel();
    assertFalse(task.isOpen());
  }

}
