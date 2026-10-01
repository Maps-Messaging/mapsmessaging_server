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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.protocol;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SatellitePublishFailureTest {

  @ParameterizedTest
  @MethodSource("failures")
  @SuppressWarnings("unchecked")
  void asynchronous_publish_failure_preserves_original_cause(Exception failure) throws Exception {
    SatelliteGatewayProtocol protocol = mock(SatelliteGatewayProtocol.class, CALLS_REAL_METHODS);
    Session session = mock(Session.class);
    CompletableFuture<Destination> lookup = mock(CompletableFuture.class);
    CompletableFuture<Void> completion = mock(CompletableFuture.class);
    when(session.findDestination("/incoming", DestinationType.TOPIC)).thenReturn(lookup);
    when(lookup.thenAccept(any())).thenReturn(completion);
    when(completion.exceptionally(any())).thenReturn(completion);
    when(completion.get(1, TimeUnit.SECONDS)).thenThrow(failure);
    Field sessionField = SatelliteGatewayProtocol.class.getDeclaredField("session");
    sessionField.setAccessible(true);
    sessionField.set(protocol, session);
    Method publish = SatelliteGatewayProtocol.class.getDeclaredMethod("publishMessage",
        byte[].class, String.class, ProtocolMessageTransformation.class, Map.class);
    publish.setAccessible(true);

    InvocationTargetException invocation = assertThrows(InvocationTargetException.class,
        () -> publish.invoke(protocol, new byte[]{1}, "/incoming", null, new HashMap<>()));

    RuntimeException wrapped = assertInstanceOf(RuntimeException.class, invocation.getCause());
    assertEquals(RuntimeException.class, wrapped.getClass(), wrapped::toString);
    assertSame(failure, wrapped.getCause());
  }

  private static Stream<Exception> failures() {
    return Stream.of(new InterruptedException("interrupted"),
        new ExecutionException(new IOException("failed")), new TimeoutException("timeout"));
  }

}
