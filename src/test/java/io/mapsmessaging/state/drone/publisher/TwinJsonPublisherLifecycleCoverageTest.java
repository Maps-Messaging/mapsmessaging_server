package io.mapsmessaging.state.drone.publisher;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.core.TwinType;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TwinJsonPublisherLifecycleCoverageTest {

  private TwinManager twinManager;
  private SessionManager sessionManager;
  private Session session;
  private Destination twinDestination;
  private Destination contactDestination;
  private LocationManager locationManager;
  private MockedStatic<SessionManager> sessionManagerStatic;
  private MockedStatic<LocationManager> locationManagerStatic;

  @BeforeEach
  void setUp() {
    twinManager = mock(TwinManager.class);
    sessionManager = mock(SessionManager.class);
    session = mock(Session.class);
    twinDestination = mock(Destination.class);
    contactDestination = mock(Destination.class);
    locationManager = mock(LocationManager.class);

    sessionManagerStatic = mockStatic(SessionManager.class);
    sessionManagerStatic.when(SessionManager::getInstance).thenReturn(sessionManager);
    when(sessionManager.createAsync(any(), any()))
        .thenReturn(CompletableFuture.completedFuture(session));

    locationManagerStatic = mockStatic(LocationManager.class);
    locationManagerStatic.when(LocationManager::getInstance).thenReturn(locationManager);
    when(locationManager.isSet()).thenReturn(false);
    when(locationManager.getLatitude()).thenReturn(0.0);
    when(locationManager.getLongitude()).thenReturn(0.0);

    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenAnswer(invocation -> {
          String topic = invocation.getArgument(0);
          Destination destination = topic.endsWith("/contacts") ? contactDestination : twinDestination;
          return CompletableFuture.completedFuture(destination);
        });
  }

  @AfterEach
  void tearDown() {
    locationManagerStatic.close();
    sessionManagerStatic.close();
    Thread.interrupted();
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, -2, -10, -100, -1000, -60000})
  void negativePublishRatesAreRejected(long publishRate) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TwinJsonPublisher(twinManager, "/twins/{twinId}", publishRate));

    verifyNoInteractions(twinManager, sessionManager);
  }

  @Test
  void constructionRegistersObserverAndCreatesInternalSession() throws Exception {
    TwinJsonPublisher publisher = publisher(0);

    verify(twinManager).addObserver(publisher);
    verify(sessionManager).createAsync(any(), same(publisher));
  }

  @Test
  void closeRemovesObserverClosesSessionAndClearsDestinationCache() throws Exception {
    TwinJsonPublisher publisher = publisher(0);
    DroneTwin twin = new DroneTwin("cache");
    publisher.publishTwin("cache", twin);
    verify(session).findDestination("/twins/DroneTwin/cache", DestinationType.TOPIC);

    publisher.close();

    verify(twinManager).removeObserver(publisher);
    verify(sessionManager).close(session, true);

    publisher.publishTwin("cache", twin);
    verify(session, times(2)).findDestination("/twins/DroneTwin/cache", DestinationType.TOPIC);
  }

  @Test
  void repeatedPublishingReusesResolvedDestination() throws Exception {
    TwinJsonPublisher publisher = publisher(0);
    DroneTwin twin = new DroneTwin("same");

    publisher.publishTwin("same", twin);
    publisher.publishTwin("same", twin);
    publisher.publishTwin("same", twin);

    verify(session, times(1)).findDestination("/twins/DroneTwin/same", DestinationType.TOPIC);
    verify(twinDestination, times(3)).storeMessage(any(Message.class));
  }

  @Test
  void failedStoreInvalidatesDestinationCacheForNextPublish() throws Exception {
    Destination first = mock(Destination.class);
    Destination second = mock(Destination.class);
    when(session.findDestination("/twins/DroneTwin/retry", DestinationType.TOPIC))
        .thenReturn(
            CompletableFuture.completedFuture(first),
            CompletableFuture.completedFuture(second));
    doThrow(new java.io.IOException("store failed"))
        .when(first).storeMessage(any(Message.class));

    TwinJsonPublisher publisher = publisher(0);
    DroneTwin twin = new DroneTwin("retry");

    assertDoesNotThrow(() -> publisher.publishTwin("retry", twin));
    publisher.publishTwin("retry", twin);

    verify(session, times(2)).findDestination("/twins/DroneTwin/retry", DestinationType.TOPIC);
    verify(first).storeMessage(any(Message.class));
    verify(second).storeMessage(any(Message.class));
  }

  @Test
  void destinationLookupFailurePropagatesWithoutCaching() throws Exception {
    CompletableFuture<Destination> failed = new CompletableFuture<>();
    failed.completeExceptionally(new java.io.IOException("lookup failed"));
    when(session.findDestination("/twins/DroneTwin/fail", DestinationType.TOPIC))
        .thenReturn(failed);

    TwinJsonPublisher publisher = publisher(0);

    assertThrows(
        ExecutionException.class,
        () -> publisher.publishTwin("fail", new DroneTwin("fail")));
    verify(twinDestination, never()).storeMessage(any());
  }

  @Test
  void onTwinUpdatedIgnoresNullAndBlankInputs() throws Exception {
    TwinJsonPublisher publisher = publisher(0);
    DroneTwin twin = new DroneTwin("ignored");

    publisher.onTwinUpdated(null, twin, null);
    publisher.onTwinUpdated("", twin, null);
    publisher.onTwinUpdated(" ", twin, null);
    publisher.onTwinUpdated("\t", twin, null);
    publisher.onTwinUpdated("\n", twin, null);
    publisher.onTwinUpdated("valid", null, null);

    verify(session, never()).findDestination(anyString(), any());
    verify(twinDestination, never()).storeMessage(any());
  }

  @Test
  void rateLimitSuppressesImmediateDuplicateButNotDifferentTwin() throws Exception {
    TwinJsonPublisher publisher = publisher(60_000);
    DroneTwin first = new DroneTwin("first");
    DroneTwin second = new DroneTwin("second");

    publisher.onTwinUpdated("first", first, null);
    publisher.onTwinUpdated("first", first, null);
    publisher.onTwinUpdated("second", second, null);

    verify(twinDestination, times(2)).storeMessage(any(Message.class));
    verify(session).findDestination("/twins/DroneTwin/first", DestinationType.TOPIC);
    verify(session).findDestination("/twins/DroneTwin/second", DestinationType.TOPIC);
  }

  @Test
  void zeroPublishRateNeverSuppressesUpdates() throws Exception {
    TwinJsonPublisher publisher = publisher(0);
    DroneTwin twin = new DroneTwin("unlimited");

    for (int i = 0; i < 12; i++) {
      publisher.onTwinUpdated("unlimited", twin, null);
    }

    verify(twinDestination, times(12)).storeMessage(any(Message.class));
  }

  @Test
  void interruptedDestinationLookupRestoresInterruptAndWrapsFailure() throws Exception {
    CompletableFuture<Destination> unresolved = new CompletableFuture<>();
    when(session.findDestination("/twins/DroneTwin/interrupted", DestinationType.TOPIC))
        .thenReturn(unresolved);
    TwinJsonPublisher publisher = publisher(0);

    Thread.currentThread().interrupt();
    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> publisher.onTwinUpdated("interrupted", new DroneTwin("interrupted"), null));

    assertInstanceOf(InterruptedException.class, failure.getCause());
    assertTrue(Thread.currentThread().isInterrupted());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "alpha",
      "bravo-01",
      "charlie_02",
      "delta.03",
      "echo:04",
      "$SYS",
      "fleet/foxtrot",
      "golf+one",
      "hotel#tag",
      "india space",
      "juliett@node",
      "kilo=11",
      "lima,12",
      "mike;13",
      "november~14",
      "oscar%15",
      "papa&16",
      "quebec(17)",
      "romeo[18]",
      "sierra{19}",
      "tango!20",
      "uniform'21",
      "victor-22",
      "whiskey-23",
      "xray-24",
      "yankee-25",
      "zulu-26",
      "drone-00000027",
      "vehicle-a-b-c",
      "unit.30.final"
  })
  void topicTemplateSubstitutesTwinTypeAndIdentifierLiterally(String twinId) throws Exception {
    TwinJsonPublisher publisher = publisher(0);

    publisher.publishTwin(twinId, new DroneTwin(twinId));

    verify(session).findDestination(
        "/twins/DroneTwin/" + twinId,
        DestinationType.TOPIC);
    verify(twinDestination).storeMessage(any(Message.class));
  }

  @Test
  void nonDroneTwinUsesConcreteTwinTypeAndDoesNotPublishContacts() throws Exception {
    TwinJsonPublisher publisher = publisher(0);
    GroundTwin twin = new GroundTwin("ground");

    publisher.publishTwin("ground", twin);

    verify(session).findDestination("/twins/GroundTwin/ground", DestinationType.TOPIC);
    verify(session, never()).findDestination(contains("/contacts"), any());
    verify(twinDestination).storeMessage(any(Message.class));
    verify(contactDestination, never()).storeMessage(any(Message.class));
  }

  @Test
  void publishedTwinMessageCarriesExpectedMessagingContract() throws Exception {
    TwinJsonPublisher publisher = publisher(0);

    publisher.publishTwin("contract", new DroneTwin("contract"));

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(twinDestination).storeMessage(captor.capture());
    Message message = captor.getValue();

    assertEquals(QualityOfService.AT_LEAST_ONCE, message.getQualityOfService());
    assertEquals("application/json", message.getContentType());
    assertEquals(
        io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_JSON_SCHEMA.toString(),
        message.getSchemaId());
    assertTrue(message.isStoreOffline());
    assertFalse(message.isRetain());
    assertNotNull(message.getOpaqueData());
    assertTrue(message.getOpaqueData().length > 0);
  }

  @Test
  void connectionMetadataIsStableAndCallbacksAreNoOps() throws Exception {
    TwinJsonPublisher publisher = publisher(0);

    assertEquals(0, publisher.getTimeOut());
    assertEquals("twin_json_publisher", publisher.getName());
    assertEquals("twin_json_publisher", publisher.getUniqueName());
    assertEquals("1.0", publisher.getVersion());
    assertEquals("internal", publisher.getProtocolName());
    assertEquals("", publisher.getAuthenticationConfig());
    assertEquals("", publisher.getRemoteIp());
    assertNull(publisher.getPrincipal());

    assertDoesNotThrow(publisher::sendKeepAlive);
    assertDoesNotThrow(() -> publisher.sendMessage(mock(io.mapsmessaging.api.MessageEvent.class)));
  }

  private TwinJsonPublisher publisher(long publishRate) throws Exception {
    return new TwinJsonPublisher(
        twinManager,
        "/twins/{twinType}/{twinId}",
        publishRate);
  }

  private static final class GroundTwin extends EntityTwin {
    private GroundTwin(String twinId) {
      super(twinId, null);
      setTwinType(TwinType.GROUND_CONTROL);
    }
  }
}
