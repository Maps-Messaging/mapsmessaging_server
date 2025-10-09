/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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
import io.mapsmessaging.network.route.select.LinkRepository;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteList implements LinkRepository {

  @Getter
  private final String name;
  private final Map<String, EndPointLink> endPointConnections;

  public RouteList(String name) {
    this.name = name;
    endPointConnections = new LinkedHashMap<>();
  }

  public void addEndPointConnection(final EndPointConnection endPointConnection) {
    endPointConnections.put(endPointConnection.getConfigName(), new EndPointLink(endPointConnection));
  }

  public void removeEndPointConnection(final EndPointConnection endPointConnection) {
    endPointConnections.remove(endPointConnection.getConfigName());
  }

  @Override
  public Collection<Link> getAllLinks() {
    return new ArrayList<>(endPointConnections.values());
  }

  public void start() {
    processCommand(COMMAND.START);
  }

  public void stop() {
    processCommand(COMMAND.STOP);
  }


  public void pause() {
    processCommand(COMMAND.PAUSE);
  }

  public void resume() {
    processCommand(COMMAND.RESUME);
  }

  private void processCommand(COMMAND command) {
    for (EndPointLink link : endPointConnections.values()) {
      switch (command) {
        case START:
          if (!link.getEndPointConnection().isStarted()) {
            link.getEndPointConnection().start();
          }
          break;
        case STOP:
          if (link.getEndPointConnection().isStarted()) {
            link.getEndPointConnection().stop();
          }
          break;
        case PAUSE:
          if (link.getEndPointConnection().isStarted()) {
            link.getEndPointConnection().pause();
          }
          break;

        case RESUME:
          if (link.getEndPointConnection().isStarted()) {
            link.getEndPointConnection().resume();
          }
          break;
      }
    }
  }

  enum COMMAND {
    START, STOP, PAUSE, RESUME
  }
}
