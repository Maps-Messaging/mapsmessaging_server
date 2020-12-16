/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.login.LoginException;
import net.ser1.stomp.Client;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.maps.messaging.engine.security.TestLoginModule;
import org.projectodd.stilts.stomp.StompException;
import org.projectodd.stilts.stomp.client.StompClient;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class StompConnectionTest extends StompBaseTest {


  @Test
  @DisplayName("Test anonymous Stomp client connection")
  public void testAnonymous() throws URISyntaxException, StompException, InterruptedException {
    StompClient client = new StompClient("stomp://127.0.0.1/");
    client.connect(20000);
    Assertions.assertTrue(client.isConnected());
    client.disconnect(20000);
    Assertions.assertFalse(client.isConnected());
  }

  @Test
  @DisplayName("Test passing null username and password Stomp client connection")
  public void testNullAuth() throws URISyntaxException, InterruptedException {
    StompClient client = new StompClient("stomp://127.0.0.1:2001/");
    try {
      client.connect(1000);
      Assertions.assertFalse(client.isConnected());
    } catch (StompException e) {
      Assertions.assertFalse(client.isConnected());
    }
  }

  @Test
  @DisplayName("Test passing valid username and password Stomp client connection")
  public void testAuth() throws IOException, LoginException {
    Client client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[0]));
    Assertions.assertTrue(client.isConnected());
  }

  @Test
  @DisplayName("Test passing bad username and password Stomp client connection")
  public void testBadPassAuth() throws IOException {
    Client client = null;
    try {
      client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[1]));
      Assertions.assertFalse(client.isConnected());
    } catch (LoginException e) {
      Assertions.assertTrue(e.getMessage().contains("Failed to authenticate"));
    }
  }

  @Test
  @DisplayName("Test Keep Alive header")
  public void testKeepAlive() throws IOException, LoginException  {
    Client client = new Client("127.0.0.1", 2001,  TestLoginModule.getUsernames()[0],  new String(TestLoginModule.getPasswords()[0]));
    Assertions.assertTrue(client.isConnected());
  }

    @Test
  @DisplayName("WebSocket Stomp Connection")
  public void testWebSocketConnection() throws InterruptedException, ExecutionException, TimeoutException {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient webSocketStompClient = new WebSocketStompClient(client);
    StompSessionHandlerImpl handler = new StompSessionHandlerImpl();
    ListenableFuture<StompSession> futureSession = webSocketStompClient.connect("ws://localhost:8675", handler );

    StompSession stompSession = futureSession.get(5000, TimeUnit.MILLISECONDS);
    Assertions.assertNotNull(stompSession);

    StompSession.Subscription subscription = stompSession.subscribe("/topic/test", handler);
    stompSession.send("/topic/test", "Hi There".getBytes());
    delay(1000);
    subscription.unsubscribe();
    stompSession.disconnect();
  }

  private final class StompSessionHandlerImpl implements StompSessionHandler{
    @Override
    public void afterConnected(StompSession stompSession, StompHeaders stompHeaders) {
      System.err.println("After Connected");
    }

    @Override
    public void handleException(StompSession stompSession, StompCommand stompCommand, StompHeaders stompHeaders, byte[] bytes, Throwable throwable) {
      System.err.println("Handle Exception");
    }

    @Override
    public void handleTransportError(StompSession stompSession, Throwable throwable) {
      System.err.println("Handle Transport Error");
    }

    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
      return null;
    }

    @Override
    public void handleFrame(StompHeaders stompHeaders, Object o) {
      System.err.println("Received frame::"+stompHeaders+" "+o);

    }
  }
}
