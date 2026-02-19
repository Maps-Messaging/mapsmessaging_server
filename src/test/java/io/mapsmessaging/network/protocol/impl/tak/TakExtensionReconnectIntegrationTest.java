/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
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

package io.mapsmessaging.network.protocol.impl.tak;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TakExtensionReconnectIntegrationTest {

  @Test
  void reconnectsAfterRemoteDropAndResumesOutbound() throws Exception {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      CountDownLatch firstMessageSeen = new CountDownLatch(1);
      CountDownLatch secondMessageSeen = new CountDownLatch(1);
      ExecutorService executor = Executors.newSingleThreadExecutor();

      Future<?> server = executor.submit(() -> {
        try {
          try (Socket first = serverSocket.accept()) {
            first.setSoTimeout(2000);
            String firstPayload = readPayload(first);
            assertTrue(firstPayload.contains("<event"));
            firstMessageSeen.countDown();
            // Drop connection to force reconnect on extension reader loop.
          }
          try (Socket second = serverSocket.accept()) {
            second.setSoTimeout(3000);
            String secondPayload = readPayload(second);
            assertTrue(secondPayload.contains("<event"));
            secondMessageSeen.countDown();
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

      TakExtension extension = null;
      try {
        extension = new TakExtension(mockEndPoint(port), buildConfig());
        extension.initialise();

        extension.outbound("ops/cot/out/test", cotMessage("u-1"));
        assertTrue(firstMessageSeen.await(3, TimeUnit.SECONDS));

        Thread.sleep(400);

        extension.outbound("ops/cot/out/test", cotMessage("u-2"));
        assertTrue(secondMessageSeen.await(4, TimeUnit.SECONDS));
      } finally {
        if (extension != null) {
          extension.close();
        }
        server.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();
      }
    }
  }

  private static EndPoint mockEndPoint(int port) {
    EndPointServerConfigDTO config = new EndPointServerConfigDTO();
    config.setUrl("tcp://127.0.0.1:" + port);
    EndPoint endPoint = mock(EndPoint.class);
    when(endPoint.getConfig()).thenReturn(config);
    return endPoint;
  }

  private static ExtensionConfigDTO buildConfig() throws Exception {
    ExtensionConfigDTO dto = new ExtensionConfigDTO();
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("payload", "cot_xml");
    config.put("framing", "xml_stream");
    config.put("reconnect_delay_ms", 100);
    config.put("reconnect_max_delay_ms", 400);
    config.put("reconnect_backoff_multiplier", 1.5d);
    config.put("reconnect_jitter_ms", 0);
    config.put("use_maps_transport", false);
    setField(dto, "config", config);
    return dto;
  }

  private static Message cotMessage(String uid) {
    String cot = """
        <event uid="%s" type="a-f-G-U-C" time="2026-02-19T09:10:00Z" start="2026-02-19T09:10:00Z" stale="2026-02-19T09:15:00Z" how="m-g">
          <point lat="1" lon="2"/>
        </event>
        """.formatted(uid);
    return new MessageBuilder().setOpaqueData(cot.getBytes(StandardCharsets.UTF_8)).build();
  }

  private static String readPayload(Socket socket) throws IOException {
    byte[] buffer = new byte[4096];
    int read = socket.getInputStream().read(buffer);
    if (read <= 0) {
      throw new IOException("No payload received");
    }
    return new String(buffer, 0, read, StandardCharsets.UTF_8);
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    var field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }
}
