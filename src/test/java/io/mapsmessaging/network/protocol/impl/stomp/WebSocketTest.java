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

import io.mapsmessaging.test.WaitForState;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

class WebSocketTest extends StompBaseTest {

  @Test
  @DisplayName("WebSocket Stomp Publish Test")
  void webSocketPublishTest() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient webSocketStompClient = new WebSocketStompClient(client);
    webSocketStompClient.setMessageConverter(new SimpleMessageConverter());
    StompSessionHandlerImpl handler = new StompSessionHandlerImpl();
    CompletableFuture<StompSession> futureSession = webSocketStompClient.connectAsync("ws://localhost:8674", handler);

    StompSession stompSession = futureSession.get(5000, TimeUnit.MILLISECONDS);
    Assertions.assertNotNull(stompSession);

    StompSession.Subscription subscription = stompSession.subscribe("/topic/test", handler);
    byte[] tmpBuffer = new byte[10];
    Arrays.fill(tmpBuffer, (byte) 0b01010101);
    for(int x=0;x<10;x++) {
      stompSession.send("/topic/test", tmpBuffer);
    }
    WaitForState.waitFor(5, TimeUnit.SECONDS, ()->handler.subscriptionCount.get() == 0);

    handler.subscriptionCount.set(10); // Reset it
    Arrays.fill(tmpBuffer, (byte) 0);
    for(int x=0;x<10;x++) {
      stompSession.send("/topic/test", tmpBuffer);
    }
    WaitForState.waitFor(5, TimeUnit.SECONDS, ()->handler.subscriptionCount.get() == 0);

    subscription.unsubscribe();
    WaitForState.wait(100, TimeUnit.MILLISECONDS);
    stompSession.disconnect();
    WaitForState.wait(100, TimeUnit.MILLISECONDS);
  }


  @Test
  @DisplayName("WebSocket Stomp Connection")
  void testWebSocketConnection() throws InterruptedException, ExecutionException, TimeoutException {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient webSocketStompClient = new WebSocketStompClient(client);
    StompSessionHandlerImpl handler = new StompSessionHandlerImpl();
    CompletableFuture<StompSession> futureSession = webSocketStompClient.connectAsync("ws://localhost:8674", handler);
    StompSession stompSession = futureSession.get(5000, TimeUnit.MILLISECONDS);
    Assertions.assertNotNull(stompSession);

    StompSession.Subscription subscription = stompSession.subscribe("/topic/test", handler);
    stompSession.send("/topic/test", "Hi There".getBytes());
    delay(1000);
    subscription.unsubscribe();
    stompSession.disconnect();
  }


  private static final class StompSessionHandlerImpl implements StompSessionHandler {
    AtomicLong subscriptionCount = new AtomicLong(10);

    @Override
    public void afterConnected(@NotNull StompSession stompSession, @NotNull StompHeaders stompHeaders) {
    }

    @Override
    public void handleException(@NotNull StompSession stompSession, StompCommand stompCommand, @NotNull StompHeaders stompHeaders, byte @NotNull [] bytes, Throwable throwable) {
      throwable.printStackTrace(System.err);
    }

    @Override
    public void handleTransportError(@NotNull StompSession stompSession, Throwable throwable) {
      throwable.printStackTrace(System.err);
    }

    @Override
    public @NotNull Type getPayloadType(@NotNull StompHeaders stompHeaders) {
      return byte[].class;
    }

    @Override
    public void handleFrame(@NotNull StompHeaders stompHeaders, Object o) {
      subscriptionCount.decrementAndGet();
    }
  }

}
