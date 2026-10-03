package io.mapsmessaging.network.protocol;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.security.auth.Subject;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtocolRoutingCoverageTest {

  private static final AtomicLong IDS = new AtomicLong();
  private HarnessProtocol protocol;

  @BeforeEach
  void setUp() {
    EndPoint endPoint = mock(EndPoint.class);
    EndPointStatus status = mock(EndPointStatus.class);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("Coverage=" + IDS.incrementAndGet()));
    when(endPoint.getEndPointStatus()).thenReturn(status);
    when(status.supportsMovingAverages()).thenReturn(false);
    protocol = new HarnessProtocol(endPoint);
  }

  @AfterEach
  void tearDown() throws Exception {
    protocol.close();
  }

  @ParameterizedTest
  @MethodSource("mappingCases")
  void inboundTopicMappingMatrix(String filter, String mapped, String topic, String expected) {
    protocol.setTopicMapping(filter, mapped);
    ParsedMessage parsed = protocol.parseInboundMessage(topic, message());
    assertEquals(expected, parsed.getDestinationName());
  }

  @ParameterizedTest
  @MethodSource("transformCases")
  void transformationLookupMatrix(String filter, String topic, boolean expectedMatch) {
    InterServerTransformation transformation = mock(InterServerTransformation.class);
    protocol.getDestinationTransformerMap().put(filter, transformation);
    InterServerTransformation result = protocol.destinationTransformationLookup(topic);
    if (expectedMatch) {
      assertSame(transformation, result);
      assertSame(transformation, protocol.getDestinationTransformerMap().get(topic));
    } else {
      assertNull(result);
    }
  }

  @ParameterizedTest
  @MethodSource("scanCases")
  void scanForNameMatrix(String filter, String mapped, String topic, String expected) {
    protocol.setTopicMapping(filter, mapped);
    assertEquals(expected, protocol.scan(topic));
  }

  @ParameterizedTest
  @MethodSource("lookupCases")
  void lookupMappingMatrix(String filter, String mapped, String input, String expected) {
    if (filter != null) {
      protocol.setTopicMapping(filter, mapped);
    }
    assertEquals(expected, protocol.parseForLookup(input));
  }

  @ParameterizedTest
  @MethodSource("builderCases")
  void subscriptionBuilderMatrix(QualityOfService qos, String selector, int receiveMaximum) {
    SubscriptionContext context =
        protocol.builder("root/topic", selector, qos, receiveMaximum).build();

    assertEquals("root/topic", context.getAlias());
    assertEquals(qos, context.getQualityOfService());
    assertEquals(qos.getClientAcknowledgement(), context.getAcknowledgementController());
    assertEquals(receiveMaximum, context.getReceiveMaximum());
    assertTrue(context.allowOverlap());
    assertEquals(selector == null || selector.isEmpty() ? null : selector, context.getSelector());
  }

  private static Message message() {
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(new byte[]{1, 2, 3});
    return builder.build();
  }

  private static Stream<Arguments> builderCases() {
    return Stream.of(
        Arguments.of(QualityOfService.AT_MOST_ONCE, null, 1),
        Arguments.of(QualityOfService.AT_MOST_ONCE, "", 10),
        Arguments.of(QualityOfService.AT_MOST_ONCE, "x = 1", 100),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, null, 1),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, "", 10),
        Arguments.of(QualityOfService.AT_LEAST_ONCE, "x = 1", 100),
        Arguments.of(QualityOfService.EXACTLY_ONCE, null, 1),
        Arguments.of(QualityOfService.EXACTLY_ONCE, "", 10),
        Arguments.of(QualityOfService.EXACTLY_ONCE, "x = 1", 100),
        Arguments.of(QualityOfService.MQTT_SN_REGISTERED, null, 1),
        Arguments.of(QualityOfService.MQTT_SN_REGISTERED, "", 10),
        Arguments.of(QualityOfService.MQTT_SN_REGISTERED, "x = 1", 100)
    );
  }

  private static Stream<Arguments> lookupCases() {
    return Stream.of(
        Arguments.of(null, null, "unchanged", "unchanged"),
        Arguments.of("exact", "mapped", "exact", "mapped"),
        Arguments.of("exact", "mapped", "other", "other"),
        Arguments.of("sensor/#", "local/", "sensor/value", "local/sensor/value"),
        Arguments.of("sensor/#", "local/", "sensor/a/b", "local/sensor/a/b"),
        Arguments.of("root/#", "mapped/", "root/value", "mapped/root/value"),
        Arguments.of("a/#", "b/", "a/value", "b/a/value"),
        Arguments.of("sensor/#", "local/", "other/value", "other/value"),
        Arguments.of("plain", "mapped", "plain", "mapped"),
        Arguments.of("plain", "mapped", "plain/child", "plain/child")
    );
  }

  private static Stream<Arguments> mappingCases() {
    return Stream.of(
        Arguments.of("a", "x", "a", "x"),
        Arguments.of("b", "y", "b", "y"),
        Arguments.of("a", "x", "b", "b"),
        Arguments.of("sensor/#", "local/", "sensor/a", "local/a"),
        Arguments.of("sensor/#", "local/", "sensor/a/b", "local/a/b"),
        Arguments.of("root/#", "mapped/", "root/1/2", "mapped/1/2"),
        Arguments.of("a/b/#", "z/", "a/b/c", "z/c"),
        Arguments.of("a/b/#", "z/", "a/b/c/d", "z/c/d"),
        Arguments.of("sensor/+/temp", "mapped/", "sensor/n1/temp", "mapped/n1/temp"),
        Arguments.of("sensor/+/temp", "mapped/", "sensor/n2/temp", "mapped/n2/temp"),
        Arguments.of("a/+/c", "x/", "a/b/c", "x/b/c"),
        Arguments.of("a/+/+", "x/", "a/b/c", "x/b/c"),
        Arguments.of("+/status", "mapped/", "node/status", "mapped/node/status"),
        Arguments.of("+/+/status", "mapped/", "site/node/status", "mapped/site/node/status"),
        Arguments.of("a/b/+", "x/", "a/b/c", "x/c"),
        Arguments.of("a/+/c/+", "x/", "a/b/c/d", "x/b/c/d"),
        Arguments.of("sensor/+/temp", "mapped/", "sensor/n1/state", "sensor/n1/state"),
        Arguments.of("sensor/+/temp", "mapped/", "sensor/temp", "sensor/temp"),
        Arguments.of("a/b/+", "x/", "a/b", "a/b"),
        Arguments.of("a/b/+", "x/", "a/b/c/d", "a/b/c/d"),
        Arguments.of("a/#/c", "x/", "a/b/c", "a/b/c"),
        Arguments.of("a/+/c", "x/", "z/b/c", "z/b/c"),
        Arguments.of("root/#", "mapped/", "other/root/a", "other/root/a"),
        Arguments.of("a/b/c", "x", "a/b", "a/b"),
        Arguments.of("a/b/c", "x", "a/b/c/d", "a/b/c/d"),
        Arguments.of("+/b", "mapped/", "a/c", "a/c"),
        Arguments.of("/sensor/#", "/local/", "/sensor/a", "/local/a"),
        Arguments.of("site/+/+/temp", "archive/", "site/a/b/temp", "archive/a/b/temp"),
        Arguments.of("a/+", "x/", "a/b", "x/b"),
        Arguments.of("root/+/status", "state/", "root/n1/status", "state/n1/status"),
        Arguments.of("root/#", "state/", "root/n1/n2", "state/n1/n2"),
        Arguments.of("fleet/#", "archive/", "fleet/uav/1", "archive/uav/1"),
        Arguments.of("fleet/#", "archive/", "fleet/usv/2", "archive/usv/2"),
        Arguments.of("fleet/+/status", "status/", "fleet/uav/status", "status/uav/status"),
        Arguments.of("fleet/+/position", "position/", "fleet/usv/position", "position/usv/position"),
        Arguments.of("site/+/+/metric", "metrics/", "site/a/b/metric", "metrics/a/b/metric"),
        Arguments.of("site/+/+/metric", "metrics/", "site/a/b/state", "site/a/b/state"),
        Arguments.of("one/two/#", "three/", "one/two/four", "three/four"),
        Arguments.of("one/two/#", "three/", "one/two/four/five", "three/four/five"),
        Arguments.of("one/+/three", "mapped/", "one/two/three", "mapped/two/three"),
        Arguments.of("one/+/three", "mapped/", "one/two/four", "one/two/four"),
        Arguments.of("alpha/beta/+", "gamma/", "alpha/beta/delta", "gamma/delta"),
        Arguments.of("alpha/beta/+", "gamma/", "alpha/beta", "alpha/beta"),
        Arguments.of("alpha/#", "omega/", "alpha/beta/gamma", "omega/beta/gamma"),
        Arguments.of("alpha/#", "omega/", "beta/alpha/gamma", "beta/alpha/gamma"),
        Arguments.of("+/tail", "head/", "node/tail", "head/node/tail")
    );
  }

  private static Stream<Arguments> transformCases() {
    return Stream.of(
        Arguments.of("a", "a", true),
        Arguments.of("b", "b", true),
        Arguments.of("sensor/#", "sensor/a", true),
        Arguments.of("sensor/#", "sensor/a/b", true),
        Arguments.of("root/#", "root/1/2", true),
        Arguments.of("sensor/+/temp", "sensor/a/temp", true),
        Arguments.of("sensor/+/temp", "sensor/b/temp", true),
        Arguments.of("a/+/c", "a/b/c", true),
        Arguments.of("+/status", "node/status", true),
        Arguments.of("a/+/+", "a/b/c", true),
        Arguments.of("a", "b", false),
        Arguments.of("sensor/#", "other/a", false),
        Arguments.of("sensor/+/temp", "sensor/a/state", false),
        Arguments.of("sensor/+/temp", "sensor/temp", false),
        Arguments.of("a/+/c", "a/b/d", false),
        Arguments.of("a/b/+", "a/b", false),
        Arguments.of("a/b/+", "a/b/c/d", false),
        Arguments.of("+/status", "node/other", false),
        Arguments.of("root/#", "roots/a", false),
        Arguments.of("a/+/+", "a/b", false)
    );
  }

  private static Stream<Arguments> scanCases() {
    return Stream.of(
        Arguments.of("sensor/#", "local/", "sensor/a", "local/a"),
        Arguments.of("sensor/#", "local/", "sensor/a/b", "local/a/b"),
        Arguments.of("root/#", "mapped/", "root/1/2", "mapped/1/2"),
        Arguments.of("a/b/#", "z/", "a/b/c", "z/c"),
        Arguments.of("/a/#", "/z/", "/a/b", "/z/b"),
        Arguments.of("sensor/#", "local/", "other/a", "other/a"),
        Arguments.of("a/b/#", "z/", "a/c/b", "a/c/b"),
        Arguments.of("#", "root/", "anything", "anything"),
        Arguments.of("plain", "mapped", "plain", "plain"),
        Arguments.of("alpha/#", "omega/", "alpha/", "omega/"),
        Arguments.of("alpha/#", "omega/", "alpha/x/y", "omega/x/y"),
        Arguments.of("x/#", "y/", "x/1", "y/1"),
        Arguments.of("x/#", "y/", "xx/1", "xx/1"),
        Arguments.of("one/two/#", "three/", "one/two/4/5", "three/4/5")
    );
  }

  private static final class HarnessProtocol extends Protocol {
    private HarnessProtocol(EndPoint endPoint) {
      super(endPoint, new ProtocolConfigDTO("coverage"));
    }

    @Override public Subject getSubject() { return null; }
    @Override public void sendMessage(MessageEvent messageEvent) { }
    @Override public boolean processPacket(Packet packet) { return false; }
    @Override public String getName() { return "ProtocolRoutingCoverage"; }
    @Override public String getSessionId() { return "coverage"; }
    @Override public String getVersion() { return "1.0"; }

    @Override
    public ProtocolInformationDTO getInformation() {
      ProtocolInformationDTO dto = new ProtocolInformationDTO();
      updateInformation(dto);
      return dto;
    }

    private String scan(String name) {
      return scanForName(name);
    }

    private SubscriptionContextBuilder builder(
        String resource, String selector, QualityOfService qos, int receiveMaximum) {
      return createSubscriptionContextBuilder(resource, selector, qos, receiveMaximum);
    }
  }
}
