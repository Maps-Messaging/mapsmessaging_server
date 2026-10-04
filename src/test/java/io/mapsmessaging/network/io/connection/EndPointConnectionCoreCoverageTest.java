/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class EndPointConnectionCoreCoverageTest {

  @Test
  void currentTransitionNotifiesListenerThenExecutesNewState() throws Exception {
    EndPointConnection connection = connection();
    State oldState = mock(State.class);
    State newState = mock(State.class);
    StateChangeListener listener = mock(StateChangeListener.class);
    connection.addStateChangeListener(listener);
    transitionSequence(connection).set(7);

    connection.commitStateChange(oldState, newState, 7).run();

    var order = inOrder(listener, newState);
    order.verify(listener).changeState(oldState, newState);
    order.verify(newState).execute();
  }

  @Test
  void staleTransitionDoesNotNotifyOrExecute() throws Exception {
    EndPointConnection connection = connection();
    State oldState = mock(State.class);
    State newState = mock(State.class);
    StateChangeListener listener = mock(StateChangeListener.class);
    connection.addStateChangeListener(listener);
    transitionSequence(connection).set(8);

    connection.commitStateChange(oldState, newState, 7).run();

    verifyNoInteractions(listener, newState);
  }

  @Test
  void listenerCanSupersedeTransitionBeforeStateExecution() throws Exception {
    EndPointConnection connection = connection();
    State oldState = mock(State.class);
    State newState = mock(State.class);
    AtomicLong sequence = transitionSequence(connection);
    sequence.set(9);
    connection.addStateChangeListener((previous, next) -> sequence.incrementAndGet());

    connection.commitStateChange(oldState, newState, 9).run();

    verify(newState, never()).execute();
  }

  @Test
  void closeCancelsCurrentStateAndClosesProtocolEndpointFallback() throws Exception {
    EndPointConnection connection = connection();
    State state = mock(State.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);
    when(protocol.getEndPoint()).thenReturn(endPoint);
    set(connection, "state", state);
    connection.setProtocol(protocol);

    assertDoesNotThrow(connection::close);

    verify(state).cancel();
    verify(endPoint).close();
  }

  private static EndPointConnection connection() {
    EndPointConnectionServerConfigDTO properties = mock(EndPointConnectionServerConfigDTO.class);
    when(properties.getProtocols()).thenReturn("mqtt");
    when(properties.getName()).thenReturn("coverage");
    return new EndPointConnection(
        new EndPointURL("tcp://127.0.0.1:1883"),
        properties,
        mock(EndPointConnectionFactory.class),
        mock(SelectorLoadManager.class),
        null);
  }

  private static AtomicLong transitionSequence(EndPointConnection connection) throws Exception {
    Field field = EndPointConnection.class.getDeclaredField("transitionSequence");
    field.setAccessible(true);
    return (AtomicLong) field.get(connection);
  }

  private static void set(EndPointConnection connection, String name, Object value) throws Exception {
    Field field = EndPointConnection.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(connection, value);
  }
}
