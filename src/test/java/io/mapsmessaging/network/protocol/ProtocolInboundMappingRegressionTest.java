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

package io.mapsmessaging.network.protocol;

import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.api.transformers.ParsedMessage;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.impl.mqtt.MQTTProtocol;
import io.mapsmessaging.network.protocol.impl.mqtt.listeners.PublishListener;
import io.mapsmessaging.network.protocol.impl.mqtt.packet.Publish;
import io.mapsmessaging.network.protocol.impl.mqtt5.MQTT5Protocol;
import io.mapsmessaging.network.protocol.impl.mqtt5.listeners.PublishListener5;
import io.mapsmessaging.network.protocol.impl.mqtt5.packet.Publish5;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.security.auth.Subject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProtocolInboundMappingRegressionTest {
  private static final AtomicLong IDS = new AtomicLong();
  private HarnessProtocol protocol;
  private EndPoint endPoint;

  @BeforeEach
  void set_up() {
    endPoint = mock(EndPoint.class);
    EndPointStatus status = mock(EndPointStatus.class);
    when(endPoint.getJMXTypePath()).thenReturn(List.of("InboundMapping=" + IDS.incrementAndGet()));
    when(endPoint.getEndPointStatus()).thenReturn(status);
    protocol = new HarnessProtocol(endPoint);
  }

  @AfterEach
  void tear_down() throws Exception {
    protocol.close();
  }

  @ParameterizedTest
  @CsvSource(value = {
      "remote/#|local/#|remote/device/temp|local/device/temp",
      "remote/#|local|remote/device/temp|local/device/temp",
      "remote/#|local/|remote/device/temp|local/device/temp",
      "remote/#|local//#|remote/device/temp|local/device/temp",
      "remote/#|''|remote/device/temp|remote/device/temp",
      "remote/#|remote/#|remote/device/temp|remote/device/temp",
      "#|''|device/temp|device/temp",
      "remote|''|remote|remote",
      "remote/#|local/#|other/device|other/device",
      "remote/#|local/#|remote|local",
      "remote/+/temp|local/#|remote/device/temp|local/device/temp",
      "remote/#|local/#|$SYS/remote/device|$SYS/remote/device",
      "#|local/#|$SYS/status|$SYS/status",
      "$SYS/#|archive/#|$SYS/status|archive/status",
      "remote/+|local/#|remote/|local/",
      "$schema/remote/#|local/#|$schema/remote/device|local/device"
  }, delimiter = '|')
  void lookup_and_inbound_replace_the_matched_prefix(String remote, String local, String source, String expected) {
    protocol.setTopicMapping(remote, local);
    assertEquals(expected, protocol.parseForLookup(source));
    assertEquals(expected, protocol.parseInboundMessage(source, message()).getDestinationName());
  }

  @ParameterizedTest
  @CsvSource(value = {
      "3|remote/#|local/#|remote/device/temp|local/device/temp",
      "5|remote/#|local/#|remote/device/temp|local/device/temp",
      "3|remote/#|''|remote/device/temp|remote/device/temp",
      "5|remote/#|''|remote/device/temp|remote/device/temp",
      "3|remote/#|remote/#|remote/device/temp|remote/device/temp",
      "5|remote/#|remote/#|remote/device/temp|remote/device/temp",
      "3|$schema/remote/#|local/#|$schema/remote/device|local/device",
      "5|$schema/remote/#|local/#|$schema/remote/device|local/device",
      "3|#|local/#|$SYS/status|BLOCKED",
      "5|#|local/#|$SYS/status|BLOCKED"
  }, delimiter = '|')
  void mqtt_pull_maps_once_and_preserves_schema_and_system_guards(
      int version, String remote, String local, String source, String expected) throws Exception {
    protocol.setTopicMapping(remote, local);
    protocol.setTopicMapping("local/#", "second/#");
    Session session = mock(Session.class);
    when(session.getName()).thenReturn("bridge");
    when(session.findDestination(anyString(), eq(DestinationType.TOPIC)))
        .thenReturn(CompletableFuture.completedFuture(null));
    Protocol mqtt = version == 3 ? mock(MQTTProtocol.class) : mock(MQTT5Protocol.class);
    when(mqtt.getProtocolConfig()).thenReturn(new ProtocolConfigDTO("mqtt"));
    when(mqtt.parseForLookup(anyString())).thenAnswer(call -> protocol.parseForLookup(call.getArgument(0)));
    when(mqtt.parseInboundMessage(anyString(), any(Message.class)))
        .thenAnswer(call -> protocol.parseInboundMessage(call.getArgument(0), call.getArgument(1)));
    if (version == 3) {
      new PublishListener().handlePacket(new Publish(false, new byte[]{1}, QualityOfService.AT_MOST_ONCE,
          1, source), session, endPoint, mqtt);
    } else {
      new PublishListener5().handlePacket(new Publish5(new byte[]{1}, QualityOfService.AT_MOST_ONCE,
          1, source, false), session, endPoint, mqtt);
    }
    if (expected.equals("BLOCKED")) {
      verify(session, never()).findDestination(anyString(), any(DestinationType.class));
    } else {
      verify(session).findDestination(expected, DestinationType.TOPIC);
    }
  }

  @Test
  void outbound_push_prefix_mapping_and_schema_passthrough_remain_unchanged() {
    protocol.setTopicMapping("local/#", "remote/");
    var subscription = new SubscriptionContextBuilder("local/#", QualityOfService.AT_MOST_ONCE.getClientAcknowledgement())
        .setQos(QualityOfService.AT_MOST_ONCE).build();
    assertEquals("remote/device", protocol.parseOutboundMessage(
        new MessageEvent("local/device", null, message(), null), subscription).getDestinationName());
    protocol.setTopicMapping("$schema/local/#", "mapped/");
    assertEquals("$schema/local/device", protocol.parseOutboundMessage(
        new MessageEvent("$schema/local/device", null, message(), null), subscription).getDestinationName());
  }

  @Test
  void inbound_mapping_selects_local_transformer_without_remapping_its_output() {
    protocol.setTopicMapping("remote/#", "local/#");
    protocol.setTopicMapping("local/#", "second/#");
    protocol.getDestinationTransformerMap().put("local/device", new InterServerTransformation() {
      @Override public ParsedMessage transform(String source, ParsedMessage message) {
        assertEquals("local/device", source);
        message.setDestinationName("local/transformed");
        return message;
      }
      @Override public InterServerTransformation build(TransformationConfigDTO config) { return this; }
      @Override public String getName() { return "RegressionTransformer"; }
      @Override public String getDescription() { return "Rename the mapped destination"; }
    });
    assertEquals("local/transformed", protocol.parseInboundMessage("remote/device", message()).getDestinationName());
  }

  @Test
  void exact_match_takes_precedence_over_wildcard() {
    protocol.setTopicMapping("remote/#", "local/#");
    protocol.setTopicMapping("remote/device", "exact");
    assertEquals("exact", protocol.parseForLookup("remote/device"));
  }

  @Test
  void large_empty_topic_levels_map_without_excessive_backtracking() {
    protocol.setTopicMapping("remote/#", "/".repeat(300000) + "local//#");
    assertTimeoutPreemptively(Duration.ofSeconds(5),
        () -> assertEquals("/local", protocol.parseForLookup("remote")));
  }

  @ParameterizedTest
  @CsvSource(value = {
      "local////#|local",
      "////#|''",
      "local#|local",
      "local///tail////#|local/tail"
  }, delimiter = '|')
  void empty_suffix_trims_only_trailing_separators(String local, String expected) {
    protocol.setTopicMapping("remote/#", local);
    assertEquals(expected, protocol.parseForLookup("remote"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\n", "\r", "\r\n", "\u0085", "\u2028", "\u2029"})
  void final_line_terminator_preserves_existing_suffix_trimming(String terminator) {
    protocol.setTopicMapping("remote/#", "local///" + terminator + "#");
    assertEquals("local" + terminator, protocol.parseForLookup("remote"));
  }

  private static Message message() {
    return new MessageBuilder().setOpaqueData(new byte[]{1}).build();
  }

  private static final class HarnessProtocol extends Protocol {
    private HarnessProtocol(EndPoint endPoint) {
      super(endPoint, new ProtocolConfigDTO("mqtt"));
    }
    @Override public Subject getSubject() { return null; }
    @Override public void sendMessage(MessageEvent event) { }
    @Override public boolean processPacket(Packet packet) { return false; }
    @Override public String getName() { return "InboundMapping"; }
    @Override public String getSessionId() { return "bridge"; }
    @Override public String getVersion() { return "1"; }
    @Override public ProtocolInformationDTO getInformation() { return new ProtocolInformationDTO(); }
  }
}
