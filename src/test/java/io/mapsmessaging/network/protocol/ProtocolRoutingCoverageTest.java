package io.mapsmessaging.network.protocol;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
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

  private static Message message() {
    MessageBuilder builder = new MessageBuilder();
    builder.setOpaqueData(new byte[]{1, 2, 3});
    return builder.build();
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
        Arguments.of("+/b", "mapped/", "a/c", "a/c")
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
  }
}
