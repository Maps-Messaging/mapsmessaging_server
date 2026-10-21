package io.mapsmessaging.state.drone.publisher;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.state.drone.core.TwinManager;
import io.mapsmessaging.state.drone.drone.DroneTwin;
import io.mapsmessaging.state.drone.model.Contact;
import io.mapsmessaging.state.drone.model.GeoPosition;
import io.mapsmessaging.utilities.GeoHashUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TwinJsonPublisherGeospatialContactCoverageTest {

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
          return CompletableFuture.completedFuture(
              topic.endsWith("/contacts") ? contactDestination : twinDestination);
        });
  }

  @AfterEach
  void tearDown() {
    locationManagerStatic.close();
    sessionManagerStatic.close();
  }

  @ParameterizedTest
  @MethodSource("worldPositionPairs")
  void publishingComputesServerAndTwinGeohashes(
      double serverLatitude,
      double serverLongitude,
      double twinLatitude,
      double twinLongitude) throws Exception {
    when(locationManager.getLatitude()).thenReturn(serverLatitude);
    when(locationManager.getLongitude()).thenReturn(serverLongitude);

    String twinId = "geo-" + Math.abs(Double.hashCode(twinLatitude) ^ Double.hashCode(twinLongitude));
    DroneTwin twin = new DroneTwin(twinId);
    twin.setGeoPosition(new GeoPosition(twinLatitude, twinLongitude, 100.0, 25.0));

    TwinJsonPublisher publisher = publisher();
    publisher.publishTwin(twinId, twin);

    assertNotNull(twin.getServerPosition());
    assertEquals(serverLatitude, twin.getServerPosition().getLatitude());
    assertEquals(serverLongitude, twin.getServerPosition().getLongitude());
    assertEquals(
        GeoHashUtils.toGeoHash(serverLatitude, serverLongitude, 12),
        twin.getServerGeoHash());
    assertEquals(
        GeoHashUtils.toGeoHash(twinLatitude, twinLongitude, 12),
        twin.getGeoHash());

    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(twinDestination).storeMessage(captor.capture());
    JsonObject json = json(captor.getValue());
    assertEquals(twinId, json.get("twinId").getAsString());
    assertEquals(twin.getServerGeoHash(), json.get("serverGeoHash").getAsString());
    assertEquals(twin.getGeoHash(), json.get("geoHash").getAsString());
  }

  @Test
  void zeroServerCoordinateDoesNotCreateServerPosition() throws Exception {
    when(locationManager.getLatitude()).thenReturn(0.0);
    when(locationManager.getLongitude()).thenReturn(151.2093);

    DroneTwin twin = new DroneTwin("equator");
    twin.setGeoPosition(new GeoPosition(-33.8688, 151.2093, null, null));

    publisher().publishTwin("equator", twin);

    assertNull(twin.getServerPosition());
    assertNull(twin.getServerGeoHash());
    assertEquals(
        GeoHashUtils.toGeoHash(-33.8688, 151.2093, 12),
        twin.getGeoHash());
  }

  @Test
  void epsilonServerCoordinatesAreIgnored() throws Exception {
    when(locationManager.getLatitude()).thenReturn(0.0000005);
    when(locationManager.getLongitude()).thenReturn(-0.0000005);

    DroneTwin twin = new DroneTwin("epsilon");

    publisher().publishTwin("epsilon", twin);

    assertNull(twin.getServerPosition());
    assertNull(twin.getServerGeoHash());
  }

  @Test
  void nullTwinPositionDoesNotCreateTwinGeohash() throws Exception {
    DroneTwin twin = new DroneTwin("no-position");

    publisher().publishTwin("no-position", twin);

    assertNull(twin.getGeoHash());
    verify(twinDestination).storeMessage(any(Message.class));
  }

  @Test
  void partialTwinPositionDoesNotCreateTwinGeohash() throws Exception {
    DroneTwin twin = new DroneTwin("partial-position");
    twin.setGeoPosition(new GeoPosition(-33.8688, null, null, null));

    publisher().publishTwin("partial-position", twin);

    assertNull(twin.getGeoHash());
    verify(twinDestination).storeMessage(any(Message.class));
  }

  @ParameterizedTest
  @MethodSource("contactCases")
  void contactProjectionPublishesIndependentContactMessage(
      String description,
      double latitude,
      double longitude,
      long ttlMillis) throws Exception {
    String twinId = "sensor-" + Integer.toUnsignedString(description.hashCode());
    DroneTwin twin = new DroneTwin(twinId);
    Contact contact =
        twin.getContactManager()
            .addContact(
                description,
                new GeoPosition(latitude, longitude, 5.0, 2.0),
                ttlMillis);

    publisher().publishTwin(twinId, twin);

    verify(session).findDestination(
        "/twins/DroneTwin/" + twinId,
        DestinationType.TOPIC);
    verify(session).findDestination(
        "/twins/DroneTwin/" + twinId + "/contacts",
        DestinationType.TOPIC);

    ArgumentCaptor<Message> twinCaptor = ArgumentCaptor.forClass(Message.class);
    ArgumentCaptor<Message> contactCaptor = ArgumentCaptor.forClass(Message.class);
    verify(twinDestination).storeMessage(twinCaptor.capture());
    verify(contactDestination).storeMessage(contactCaptor.capture());

    Message contactMessage = contactCaptor.getValue();
    assertEquals(QualityOfService.AT_MOST_ONCE, contactMessage.getQualityOfService());
    assertEquals("application/json", contactMessage.getContentType());
    assertEquals(
        io.mapsmessaging.engine.schema.SchemaManager.DEFAULT_JSON_SCHEMA.toString(),
        contactMessage.getSchemaId());
    assertFalse(contactMessage.isRetain());
    assertFalse(contactMessage.isStoreOffline());

    JsonObject projected = json(contactMessage);
    assertEquals(twinId, projected.get("twinId").getAsString());
    assertFalse(projected.has("contactManager"));
    assertTrue(projected.has("contact"));

    JsonObject projectedContact = projected.getAsJsonObject("contact");
    assertEquals(contact.getId().toString(), projectedContact.get("id").getAsString());
    assertEquals(description, projectedContact.get("description").getAsString());
    assertEquals(ttlMillis, projectedContact.get("ttlMillis").getAsLong());
    JsonObject position = projectedContact.getAsJsonObject("position");
    assertEquals(latitude, position.get("latitude").getAsDouble());
    assertEquals(longitude, position.get("longitude").getAsDouble());

    JsonObject main = json(twinCaptor.getValue());
    assertFalse(main.has("contact"));
  }

  @Test
  void multipleContactsReuseContactDestinationAndPublishEachProjection() throws Exception {
    DroneTwin twin = new DroneTwin("multi");
    for (int i = 0; i < 12; i++) {
      twin.getContactManager().addContact(
          "contact-" + i,
          new GeoPosition(-33.0 - i * 0.01, 151.0 + i * 0.01, null, null),
          0);
    }

    publisher().publishTwin("multi", twin);

    verify(session, times(1)).findDestination(
        "/twins/DroneTwin/multi/contacts",
        DestinationType.TOPIC);
    ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
    verify(contactDestination, times(12)).storeMessage(captor.capture());

    assertEquals(
        12,
        captor.getAllValues().stream()
            .map(this::json)
            .map(object -> object.getAsJsonObject("contact").get("description").getAsString())
            .distinct()
            .count());
  }

  @Test
  void emptyContactListDoesNotResolveContactDestination() throws Exception {
    DroneTwin twin = new DroneTwin("empty");

    publisher().publishTwin("empty", twin);

    verify(session, never()).findDestination(endsWith("/contacts"), eq(DestinationType.TOPIC));
    verify(contactDestination, never()).storeMessage(any(Message.class));
    verify(twinDestination).storeMessage(any(Message.class));
  }

  @Test
  void contactAndTwinMessagesUseDifferentDeliverySemantics() throws Exception {
    DroneTwin twin = new DroneTwin("delivery");
    twin.getContactManager().addContact(
        "target",
        new GeoPosition(10.0, 20.0, null, null),
        0);

    publisher().publishTwin("delivery", twin);

    ArgumentCaptor<Message> twinCaptor = ArgumentCaptor.forClass(Message.class);
    ArgumentCaptor<Message> contactCaptor = ArgumentCaptor.forClass(Message.class);
    verify(twinDestination).storeMessage(twinCaptor.capture());
    verify(contactDestination).storeMessage(contactCaptor.capture());

    assertEquals(QualityOfService.AT_LEAST_ONCE, twinCaptor.getValue().getQualityOfService());
    assertTrue(twinCaptor.getValue().isStoreOffline());
    assertEquals(QualityOfService.AT_MOST_ONCE, contactCaptor.getValue().getQualityOfService());
    assertFalse(contactCaptor.getValue().isStoreOffline());
  }

  @Test
  void contactDestinationIsCachedAcrossTwinRepublishes() throws Exception {
    DroneTwin twin = new DroneTwin("contact-cache");
    twin.getContactManager().addContact(
        "target",
        new GeoPosition(1.0, 2.0, null, null),
        0);
    TwinJsonPublisher publisher = publisher();

    publisher.publishTwin("contact-cache", twin);
    publisher.publishTwin("contact-cache", twin);
    publisher.publishTwin("contact-cache", twin);

    verify(session, times(1)).findDestination(
        "/twins/DroneTwin/contact-cache",
        DestinationType.TOPIC);
    verify(session, times(1)).findDestination(
        "/twins/DroneTwin/contact-cache/contacts",
        DestinationType.TOPIC);
    verify(twinDestination, times(3)).storeMessage(any(Message.class));
    verify(contactDestination, times(3)).storeMessage(any(Message.class));
  }

  private TwinJsonPublisher publisher() throws Exception {
    return new TwinJsonPublisher(
        twinManager,
        "/twins/{twinType}/{twinId}",
        0);
  }

  private JsonObject json(Message message) {
    return JsonParser.parseString(
            new String(message.getOpaqueData(), StandardCharsets.UTF_8))
        .getAsJsonObject();
  }

  private static Stream<Arguments> worldPositionPairs() {
    double[][] coordinates = {
        {-33.8688, 151.2093},
        {51.5074, -0.1278},
        {40.7128, -74.0060},
        {35.6762, 139.6503},
        {1.3521, 103.8198},
        {-36.8485, 174.7633},
        {-22.9068, -43.1729},
        {37.7749, -122.4194},
        {48.8566, 2.3522},
        {52.5200, 13.4050},
        {55.7558, 37.6173},
        {19.4326, -99.1332},
        {28.6139, 77.2090},
        {-1.2921, 36.8219},
        {-33.9249, 18.4241},
        {25.2048, 55.2708},
        {31.2304, 121.4737},
        {22.3193, 114.1694},
        {41.0082, 28.9784},
        {59.3293, 18.0686},
        {60.1699, 24.9384},
        {64.1466, -21.9426},
        {43.6532, -79.3832},
        {49.2827, -123.1207},
        {-34.6037, -58.3816},
        {-12.0464, -77.0428},
        {-23.5505, -46.6333},
        {13.7563, 100.5018},
        {21.0278, 105.8342},
        {-6.2088, 106.8456},
        {3.1390, 101.6869},
        {-27.4698, 153.0251}
    };

    return IntStream.range(0, coordinates.length)
        .mapToObj(index -> {
          double[] server = coordinates[index];
          double[] twin = coordinates[(index + 7) % coordinates.length];
          return Arguments.of(server[0], server[1], twin[0], twin[1]);
        });
  }

  private static Stream<Arguments> contactCases() {
    List<String> descriptions = List.of(
        "vehicle",
        "small-craft",
        "person",
        "unknown target",
        "red buoy",
        "blue buoy",
        "RHIB-01",
        "contact/alpha",
        "contact:bravo",
        "contact_charlie",
        "surface vessel",
        "fixed object",
        "moving object",
        "camera detection",
        "radar detection",
        "ais correlation",
        "thermal target",
        "visual target",
        "north-sector",
        "south-sector",
        "east-sector",
        "west-sector",
        "target-23",
        "target-24",
        "target-25",
        "target-26",
        "target-27",
        "target-28",
        "target-29",
        "target-30",
        "target-31",
        "target-32");

    return IntStream.range(0, descriptions.size())
        .mapToObj(index -> {
          double latitude = -40.0 + index * 2.25;
          double longitude = 110.0 + index * 1.5;
          long ttl = (index % 2 == 0) ? 0L : 60_000L;
          return Arguments.of(descriptions.get(index), latitude, longitude, ttl);
        });
  }
}
