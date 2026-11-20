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

package io.mapsmessaging.network.protocol.impl.mqtt5.listeners;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.impl.mqtt5.AuthenticationContext;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.TopicAliasMapping;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.ConnAck5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Connect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Disconnect5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.StatusCode;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationMethod;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.AuthenticationData;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.MessagePropertyFactory;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.properties.SubscriptionIdentifier;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class Mqtt5ListenerHardeningTest {

  @Test
  void rejectsUnsupportedEnhancedAuthWithoutCreatingSession() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    Connect5 connect = new Connect5();
    connect.add(new AuthenticationMethod("unsupported"));

    ConnAck5 rejected = assertInstanceOf(ConnAck5.class,
        new ConnectListener5().handlePacket(connect, null, endPoint, protocol));

    assertEquals(StatusCode.BAD_AUTHENTICATION_METHOD, rejected.getStatusCode());
    verify(protocol).setClosing(true);
    rejected.complete();
    verify(endPoint).close();
  }

  @Test
  void rejectsDifferentMethodWhenSaslIsConfigured() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    AuthenticationContext context = mock(AuthenticationContext.class);
    when(context.getAuthMethod()).thenReturn("SCRAM-SHA-256");
    when(protocol.getAuthenticationContext()).thenReturn(context);
    EndPoint endPoint = mock(EndPoint.class);
    Connect5 connect = new Connect5();
    connect.add(new AuthenticationMethod("unsupported"));

    ConnAck5 rejected = assertInstanceOf(ConnAck5.class,
        new ConnectListener5().handlePacket(connect, null, endPoint, protocol));

    assertEquals(StatusCode.BAD_AUTHENTICATION_METHOD, rejected.getStatusCode());
    rejected.complete();
    verify(endPoint).close();
  }

  @Test
  void asynchronousConnAckIncludesFinalSaslData() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    AuthenticationContext context = mock(AuthenticationContext.class);
    when(protocol.getAuthenticationContext()).thenReturn(context);
    when(protocol.getClientTopicAliasMapping()).thenReturn(new TopicAliasMapping("Client"));
    when(context.getAuthenticationMethod()).thenReturn(new AuthenticationMethod("SCRAM-SHA-256"));
    byte[] serverFinal = {1, 2, 3, 4};
    when(context.getFinalAuthenticationData()).thenReturn(serverFinal);

    Session session = mock(Session.class);
    ConnAck5 connAck = new ConnAck5();
    Method login = ConnectListener5.class.getDeclaredMethod("login", Session.class, ConnAck5.class, MQTT5Protocol.class);
    login.setAccessible(true);
    login.invoke(new ConnectListener5(), session, connAck, protocol);

    assertEquals(StatusCode.SUCCESS, connAck.getStatusCode());
    AuthenticationData authentication = (AuthenticationData) connAck.getProperties().get(MessagePropertyFactory.AUTHENTICATION_DATA);
    assertArrayEquals(serverFinal, authentication.getAuthenticationData());
    verify(session).login();
  }

  @Test
  void plainConnAckDoesNotIncludeSaslData() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    when(protocol.getClientTopicAliasMapping()).thenReturn(new TopicAliasMapping("Client"));
    Session session = mock(Session.class);
    ConnAck5 connAck = new ConnAck5();
    Method login = ConnectListener5.class.getDeclaredMethod("login", Session.class, ConnAck5.class, MQTT5Protocol.class);
    login.setAccessible(true);
    login.invoke(new ConnectListener5(), session, connAck, protocol);
    assertNull(connAck.getProperties().get(MessagePropertyFactory.AUTHENTICATION_DATA));
  }

  @Test
  void serverRejectsClientPublishContainingSubscriptionIdentifier() throws Exception {
    MQTT5Protocol protocol = mock(MQTT5Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    Publish5 publish = new Publish5(new byte[] {1}, QualityOfService.AT_LEAST_ONCE,
        42, "topic/test", false);
    publish.add(new SubscriptionIdentifier(9));

    Disconnect5 rejection = assertInstanceOf(Disconnect5.class,
        new PublishListener5().handlePacket(publish, null, endPoint, protocol));

    assertEquals(StatusCode.PROTOCOL_ERROR, rejection.getDisconnectReason());
    assertTrue(rejection.getCallback() != null);
    verify(protocol).setClosing(true);
  }
}
