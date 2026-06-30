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

package io.mapsmessaging.engine.schema;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkFormatManagerTest {

  private final LinkFormatManager manager = LinkFormatManager.getInstance();

  @Test
  void getInstance_returnsSingleton() {
    assertSame(manager, LinkFormatManager.getInstance());
  }

  @Test
  void buildLinkFormatString_serializesEligibleLinksInInputOrder() {
    List<LinkFormat> links = List.of(
        new LinkFormat("sensors/temperature", "sensor", "temperature"),
        new LinkFormat("sensors/humidity", "sensor", "humidity"));

    String result = manager.buildLinkFormatString("", links);

    String[] packedLinks = result.split(",");
    assertEquals(2, packedLinks.length);
    assertTrue(packedLinks[0].contains("<sensors/temperature>"));
    assertTrue(packedLinks[0].contains("if=\"sensor\""));
    assertTrue(packedLinks[0].contains("rt=\"temperature\""));
    assertTrue(packedLinks[1].contains("<sensors/humidity>"));
    assertTrue(packedLinks[1].contains("rt=\"humidity\""));
  }

  @Test
  void buildLinkFormatString_excludesSystemDiscoveryAndMissingInterfaceLinks() {
    List<LinkFormat> links = List.of(
        new LinkFormat("$SYS/metrics", "monitor", "numeric"),
        new LinkFormat("$sys/health", "monitor", "status"),
        new LinkFormat(".WELL-KNOWN/CORE", "discovery", "core"),
        new LinkFormat("sensors/private", null, "temperature"),
        new LinkFormat("sensors/public", "sensor", "temperature"));

    String result = manager.buildLinkFormatString(null, links);

    assertTrue(result.contains("<sensors/public>"));
    assertFalse(result.contains("$SYS"));
    assertFalse(result.contains("$sys"));
    assertFalse(result.contains(".WELL-KNOWN/CORE"));
    assertFalse(result.contains("sensors/private"));
  }

  @Test
  void buildLinkFormatString_validSelectorCanRejectAllLinks() {
    String result = manager.buildLinkFormatString(
        "true = false",
        List.of(new LinkFormat("sensors/temperature", "sensor", "temperature")));

    assertEquals("", result);
  }

  @Test
  void buildLinkFormatString_invalidSelectorFallsBackToDefaultEligibilityRules() {
    String result = manager.buildLinkFormatString(
        "this is not a valid selector {",
        List.of(new LinkFormat("sensors/temperature", "sensor", "temperature")));

    assertTrue(result.contains("<sensors/temperature>"));
  }
}
