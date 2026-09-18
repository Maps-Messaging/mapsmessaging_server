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

package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.network.route.select.CostWeights;
import io.mapsmessaging.network.route.select.DefaultCostFunction;
import io.mapsmessaging.network.route.select.LinkSelector;
import io.mapsmessaging.network.route.select.LinkSwitcher;
import io.mapsmessaging.network.route.select.SelectionPolicy;
import io.mapsmessaging.network.route.select.SelectionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricsRouteSelectionTest {

  @Test
  void stale_current_link_switches_to_fresh_link_using_endpoint_millisecond_timestamps() {
    long now = System.currentTimeMillis();
    EndPointLink stale = link("stale", now - 10_000L);
    EndPointLink fresh = link("fresh", now);
    AtomicReference<Link> current = new AtomicReference<>(stale);

    LinkSwitcher switcher = new LinkSwitcher() {
      @Override
      public Link getCurrentLink() {
        return current.get();
      }

      @Override
      public boolean switchTo(Link nextLink, String reason) {
        current.set(nextLink);
        return true;
      }
    };

    LinkSelector selector = new LinkSelector(
        new DefaultCostFunction(),
        CostWeights.builder().build(),
        SelectionPolicy.builder().build(),
        () -> List.of(stale, fresh),
        switcher);

    SelectionResult result = selector.evaluateOnce();

    assertTrue(result.switched());
    assertEquals(fresh.getLinkId(), result.bestLinkId());
    assertEquals("switched-for-better-cost", result.reason());
    assertTrue(result.currentCost() > result.bestCost());
    assertSame(fresh, current.get());
  }

  private EndPointLink link(String name, long lastActivityMillis) {
    EndPointConnection connection = mock(EndPointConnection.class);
    EndPointConnectionServerConfigDTO properties = mock(EndPointConnectionServerConfigDTO.class);
    State state = mock(State.class);
    Protocol protocol = mock(Protocol.class);
    EndPoint endPoint = mock(EndPoint.class);

    when(connection.getConfigName()).thenReturn(name);
    when(connection.getProperties()).thenReturn(properties);
    when(properties.getCost()).thenReturn(0.0);
    when(connection.getState()).thenReturn(state);
    when(state.getLinkState()).thenReturn(LinkState.CONNECTED);
    when(connection.getProtocol()).thenReturn(protocol);
    when(protocol.getEndPoint()).thenReturn(endPoint);
    when(endPoint.getLastRead()).thenReturn(lastActivityMillis);
    when(endPoint.getLastWrite()).thenReturn(lastActivityMillis);

    return new EndPointLink(connection);
  }
}
