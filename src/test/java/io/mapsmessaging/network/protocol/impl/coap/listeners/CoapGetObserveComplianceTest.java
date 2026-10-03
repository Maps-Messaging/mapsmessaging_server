package io.mapsmessaging.network.protocol.impl.coap.listeners;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.coap.CoapProtocol;
import io.mapsmessaging.network.protocol.impl.coap.packet.BasePacket;
import io.mapsmessaging.network.protocol.impl.coap.packet.Code;
import io.mapsmessaging.network.protocol.impl.coap.packet.PacketFactory;
import io.mapsmessaging.network.protocol.impl.coap.packet.TYPE;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.Observe;
import io.mapsmessaging.network.protocol.impl.coap.packet.options.UriPath;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.Context;
import io.mapsmessaging.network.protocol.impl.coap.subscriptions.SubscriptionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoapGetObserveComplianceTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 15, 255, 65535})
  void missingResourceReturnsNotFoundWithOriginalMessageId(int messageId) throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    Session session = mock(Session.class);
    when(protocol.getSession()).thenReturn(session);
    when(session.findDestination("/missing", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));
    BasePacket request = request(TYPE.CON, messageId, "/missing");

    BasePacket response = new GetListener().handle(request, protocol);

    assertNotNull(response);
    assertEquals(Code.NOT_FOUND, response.getCode());
    assertEquals(TYPE.ACK, response.getType());
    assertEquals(messageId, response.getMessageId());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 7, 42, 255, 1024})
  void nonConfirmableMissingResourceReturnsNonConfirmableNotFound(int messageId) throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    Session session = mock(Session.class);
    when(protocol.getSession()).thenReturn(session);
    when(session.findDestination("/missing", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(null));
    BasePacket request = request(TYPE.NON, messageId, "/missing");

    BasePacket response = new GetListener().handle(request, protocol);

    assertNotNull(response);
    assertEquals(Code.NOT_FOUND, response.getCode());
    assertEquals(TYPE.NON, response.getType());
    assertEquals(messageId, response.getMessageId());
  }

  @Test
  void observeCancellationRemovesOnlyMatchingObservationAndSubscription() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    Session session = mock(Session.class);
    SubscriptionState subscriptions = mock(SubscriptionState.class);
    when(protocol.getSession()).thenReturn(session);
    when(protocol.getSubscriptionState()).thenReturn(subscriptions);
    BasePacket request = request(TYPE.CON, 77, "/sensor");
    request.getOptions().putOption(new Observe(1));

    BasePacket response = new GetListener().handle(request, protocol);

    assertNotNull(response);
    assertEquals(Code.CONTENT, response.getCode());
    assertEquals(TYPE.ACK, response.getType());
    verify(subscriptions).remove("/sensor");
    verify(session).removeSubscription("/sensor");
    verify(protocol, never()).close();
  }

  @Test
  void resetForGetDoesNotCloseWholeCoapPeer() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    BasePacket reset = request(TYPE.RST, 88, "/sensor");

    new GetListener().handle(reset, protocol);

    verify(protocol, never()).close();
  }

  @Test
  void observeRegistrationCreatesSubscriptionAndRetainsRequestToken() throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    SubscriptionState subscriptions = mock(SubscriptionState.class);
    Context context = mock(Context.class);
    SubscribedEventManager manager = mock(SubscribedEventManager.class);
    when(protocol.getSession()).thenReturn(session);
    when(protocol.getSubscriptionState()).thenReturn(subscriptions);
    when(session.findDestination("/sensor", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(subscriptions.create(eq("/sensor"), any(BasePacket.class))).thenReturn(context);
    when(session.addSubscription(any(SubscriptionContext.class))).thenReturn(manager);
    BasePacket request = request(TYPE.CON, 99, "/sensor");
    request.setToken(new byte[]{1, 2, 3});
    request.getOptions().putOption(new Observe(0));

    BasePacket response = new GetListener().handle(request, protocol);

    assertNull(response);
    ArgumentCaptor<BasePacket> sent = ArgumentCaptor.forClass(BasePacket.class);
    verify(protocol).sendResponse(sent.capture());
    BasePacket wait = sent.getValue();
    assertEquals(TYPE.ACK, wait.getType());
    assertEquals(Code.EMPTY, wait.getCode());
    assertArrayEquals(new byte[]{1, 2, 3}, request.getToken());

    ArgumentCaptor<SubscriptionContext> subscription =
        ArgumentCaptor.forClass(SubscriptionContext.class);
    verify(session).addSubscription(subscription.capture());
    assertEquals("/sensor", subscription.getValue().getFilter());
    verify(context).setSubscribedEventManager(manager);
  }

  @Test
  void nonObserveGetCreatesOneShotSubscriptionWithoutObserveOptionInWaitResponse()
      throws Exception {
    CoapProtocol protocol = mock(CoapProtocol.class);
    Session session = mock(Session.class);
    Destination destination = mock(Destination.class);
    SubscriptionState subscriptions = mock(SubscriptionState.class);
    Context context = mock(Context.class);
    when(protocol.getSession()).thenReturn(session);
    when(protocol.getSubscriptionState()).thenReturn(subscriptions);
    when(session.findDestination("/once", io.mapsmessaging.api.features.DestinationType.TOPIC))
        .thenReturn(CompletableFuture.completedFuture(destination));
    when(subscriptions.create(eq("/once"), any(BasePacket.class))).thenReturn(context);
    when(session.addSubscription(any(SubscriptionContext.class)))
        .thenReturn(mock(SubscribedEventManager.class));
    BasePacket request = request(TYPE.CON, 100, "/once");

    BasePacket response = new GetListener().handle(request, protocol);

    assertNull(response);
    ArgumentCaptor<BasePacket> sent = ArgumentCaptor.forClass(BasePacket.class);
    verify(protocol).sendResponse(sent.capture());
    assertFalse(sent.getValue().getOptions().hasOption(
        io.mapsmessaging.network.protocol.impl.coap.packet.options.Constants.OBSERVE));
  }

  private BasePacket request(TYPE type, int messageId, String path) {
    BasePacket request =
        new BasePacket(PacketFactory.GET, type, Code.valueOf((byte) 1), 1, messageId, new byte[0]);
    UriPath uriPath = new UriPath();
    uriPath.setPath(path);
    request.getOptions().putOption(uriPath);
    return request;
  }
}
