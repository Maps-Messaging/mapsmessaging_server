/*
 * Copyright [ 2020 - 2026 ] Matthew Buckton
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mapsmessaging.network.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.FakeProtocol;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Link mappings are directional. A push link (subscribeLocal) must only rewrite OUTBOUND
 * names and a pull link (subscribeRemote) must only rewrite INBOUND names. Observed live on
 * 2026-09-06 with one connection carrying both: a message pulled from the partner on
 * 4817/catl/maps/json/USV-001/MessageTypeEnum_TASK_ADMIN was delivered locally as
 * 4817/catl/maps/json/4817/catl/maps/json/USV-001/... because the push entry
 * (4817/catl/maps/json/# -> 4817/catl/maps/json/) was applied to the inbound publish, and
 * the rewritten name then matched the push filter and went straight back out.
 */
class ProtocolLinkMappingTest {

  private static final String TASK_TOPIC = "4817/catl/maps/json/USV-001/MessageTypeEnum_TASK_ADMIN";

  private static FakeProtocol mixedLinks() throws IOException {
    FakeProtocol protocol = new FakeProtocol(event -> {});
    // the two pushes of the partner connection
    protocol.subscribeLocal("4817/catl/maps/json/#", "4817/catl/maps/json/", QualityOfService.AT_LEAST_ONCE, null, null, null, null, null);
    protocol.subscribeLocal("4817/catl/maps/proto/#", "sandbox/maps/4817/catl/maps/proto/", QualityOfService.AT_LEAST_ONCE, null, null, null, null, null);
    // the tasking pull on the same connection
    protocol.subscribeRemote("4817/catl/maps/json/+/MessageTypeEnum_TASK_ADMIN", "/incoming/", QualityOfService.AT_LEAST_ONCE, null, null, null, null);
    return protocol;
  }

  @Test
  void inboundPublishIsNotRewrittenByPushEntries() throws IOException {
    FakeProtocol protocol = mixedLinks();
    // parseForLookup is what the MQTT publish listeners apply to an inbound PUBLISH first
    String lookup = protocol.parseForLookup(TASK_TOPIC);
    assertEquals(TASK_TOPIC, lookup, "a pull with a '+' filter leaves the name to the wildcard mapping; the push '#' entry must not prepend its remote namespace");
    ParsedMessage parsed = protocol.parseInboundMessage(lookup, null);
    assertEquals("/incoming/USV-001/MessageTypeEnum_TASK_ADMIN", parsed.getDestinationName());
  }

  @Test
  void hashPullStillPrependsItsLocalNamespaceOnInbound() throws IOException {
    FakeProtocol protocol = mixedLinks();
    protocol.subscribeRemote("4817/#", "/cop/", QualityOfService.AT_LEAST_ONCE, null, null, null, null);
    assertEquals("/cop/4817/catl/inesctec/x", protocol.parseForLookup("4817/catl/inesctec/x"));
    // and the push entry for the same prefix does not get a say on the way in
    assertEquals("/cop/4817/catl/maps/json/USV-001/MessageTypeEnum_NODE_STATUS",
        protocol.parseForLookup("4817/catl/maps/json/USV-001/MessageTypeEnum_NODE_STATUS"));
  }

  @Test
  void directionalViewsSeparatePushFromPullAndKeepStaticEntries() throws IOException {
    FakeProtocol protocol = mixedLinks();
    protocol.setTopicMapping("static/in", "static/out");   // an endpoint-level mapping, no direction
    Map<String, String> outbound = protocol.outboundMappings();
    Map<String, String> inbound = protocol.inboundMappings();
    assertTrue(outbound.containsKey("4817/catl/maps/json/#"));
    assertFalse(outbound.containsKey("4817/catl/maps/json/+/MessageTypeEnum_TASK_ADMIN"));
    assertTrue(inbound.containsKey("4817/catl/maps/json/+/MessageTypeEnum_TASK_ADMIN"));
    assertFalse(inbound.containsKey("4817/catl/maps/json/#"));
    assertEquals("static/out", outbound.get("static/in"));
    assertEquals("static/out", inbound.get("static/in"));
    // the legacy combined map still carries everything (JMX, satellite protocols)
    assertEquals(4, protocol.getTopicNameMapping().size());
  }
}
