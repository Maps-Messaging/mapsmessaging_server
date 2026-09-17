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

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.Link;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteListTest {

  @Test
  void addAndRemoveConnectionUpdatesRepository() {
    EndPointConnection connection = connection("primary", false);
    RouteList routeList = new RouteList("route-a");

    routeList.addEndPointConnection(connection);

    Collection<Link> links = routeList.getAllLinks();
    assertEquals(1, links.size());
    assertSame(connection, ((EndPointLink) links.iterator().next()).getEndPointConnection());
    assertSame(links.iterator().next(), routeList.getLink(connection));

    routeList.removeEndPointConnection(connection);

    assertEquals(0, routeList.getAllLinks().size());
    assertNull(routeList.getLink(connection));
  }

  @Test
  void startOnlyStartsConnectionsThatAreNotStarted() {
    EndPointConnection stopped = connection("stopped", false);
    EndPointConnection started = connection("started", true);
    RouteList routeList = new RouteList("route-a");
    routeList.addEndPointConnection(stopped);
    routeList.addEndPointConnection(started);

    routeList.start();

    verify(stopped).start();
    verify(started, never()).start();
  }

  @Test
  void stopOnlyStopsStartedConnections() {
    EndPointConnection stopped = connection("stopped", false);
    EndPointConnection started = connection("started", true);
    RouteList routeList = new RouteList("route-a");
    routeList.addEndPointConnection(stopped);
    routeList.addEndPointConnection(started);

    routeList.stop();

    verify(started).stop();
    verify(stopped, never()).stop();
  }

  @Test
  void pauseAndResumeOnlyAffectStartedConnections() {
    EndPointConnection stopped = connection("stopped", false);
    EndPointConnection started = connection("started", true);
    RouteList routeList = new RouteList("route-a");
    routeList.addEndPointConnection(stopped);
    routeList.addEndPointConnection(started);

    routeList.pause();
    routeList.resume();

    verify(started).pause();
    verify(started).resume();
    verify(stopped, never()).pause();
    verify(stopped, never()).resume();
  }

  @Test
  void addingSameConfigNameReplacesExistingLink() {
    EndPointConnection first = connection("primary", false);
    EndPointConnection replacement = connection("primary", false);
    RouteList routeList = new RouteList("route-a");

    routeList.addEndPointConnection(first);
    routeList.addEndPointConnection(replacement);

    assertEquals(1, routeList.getAllLinks().size());
    assertNull(routeList.getLink(first));
    assertSame(replacement, ((EndPointLink) routeList.getAllLinks().iterator().next()).getEndPointConnection());
  }

  private EndPointConnection connection(String configName, boolean started) {
    EndPointConnection connection = mock(EndPointConnection.class);
    when(connection.getConfigName()).thenReturn(configName);
    when(connection.isStarted()).thenReturn(started);
    return connection;
  }
}
