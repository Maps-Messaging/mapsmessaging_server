package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisconnectedTest {

  @Test
  void staleConnectionAttemptClosesDeliveredEndpoint() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    State anotherState = mock(State.class);
    when(connection.getState()).thenReturn(anotherState);
    EndPoint endpoint = mock(EndPoint.class);

    new Disconnected(connection).connected(endpoint);

    verify(endpoint).close();
    verify(connection, never()).setProtocol(any());
  }

  @Test
  void cancelClosesActiveEndpointEvenWhenCloseFails() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Disconnected state = new Disconnected(connection);
    EndPoint endpoint = mock(EndPoint.class);
    doThrow(new java.io.IOException("close")).when(endpoint).close();

    java.lang.reflect.Field field =
        Disconnected.class.getDeclaredField("activeEndPoint");
    field.setAccessible(true);
    field.set(state, endpoint);

    assertDoesNotThrow(state::cancel);
    verify(endpoint).close();
  }

  @Test
  @SuppressWarnings("unchecked")
  void topicMapPreservesPullDirectionAndStripsPushWildcard() throws Exception {
    EndPointConnection connection = mock(EndPointConnection.class);
    Disconnected state = new Disconnected(connection);

    LinkConfigDTO pull = mock(LinkConfigDTO.class);
    when(pull.getDirection()).thenReturn("pull");
    when(pull.getLocalNamespace()).thenReturn("/local/in");
    when(pull.getRemoteNamespace()).thenReturn("/remote/in/#");

    LinkConfigDTO push = mock(LinkConfigDTO.class);
    when(push.getDirection()).thenReturn("push");
    when(push.getLocalNamespace()).thenReturn("/local/out");
    when(push.getRemoteNamespace()).thenReturn("/remote/out/#");

    Method method = Disconnected.class.getDeclaredMethod("getTopicMap", List.class);
    method.setAccessible(true);
    Map<String,String> topics =
        (Map<String,String>) method.invoke(state, List.of(pull, push));

    assertEquals("/remote/in/#", topics.get("/local/in"));
    assertEquals("/remote/out/", topics.get("/local/out"));
  }

  @Test
  void metadataRepresentsDisconnectedLink() {
    Disconnected state = new Disconnected(mock(EndPointConnection.class));

    assertEquals("Disconnected", state.getName());
    assertEquals(LinkState.DISCONNECTED, state.getLinkState());
  }
}
