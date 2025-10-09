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
import io.mapsmessaging.network.io.connection.StateChangeListener;
import io.mapsmessaging.network.io.connection.state.Establishing;
import io.mapsmessaging.network.io.connection.state.Holding;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.select.*;
import lombok.Getter;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class RouteManager implements LinkSwitcher, StateChangeListener {
  private final String routeName;
  private AtomicReference<Link> currentLink;

  private final RouteList routeList;
  private final LinkSelector selector;
  private final SelectionOrchestrator orchestrator;

  public RouteManager(String routeName) {
    this.routeName = routeName;
    currentLink = new AtomicReference<>(null);
    routeList = new RouteList(routeName);
    selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(SelectionPolicy.builder().build())
        .linkRepository(routeList)
        .linkSwitcher(this)
        .build();
    orchestrator  = SelectionOrchestrator.start(selector, SelectionOrchestratorConfig.builder().build());
  }

  public void start() {
    routeList.start();
  }

  public void stop() {
    routeList.stop();
  }

  public void pause() {
    routeList.pause();
  }

  public void resume() {
    routeList.resume();
  }

  public void addEndPointConnection(EndPointConnection endPointConnection) {
    endPointConnection.setEstablishingState(new Holding(endPointConnection)); // Stops it moving to established
    endPointConnection.addStateChangeListener(this);
    routeList.addEndPointConnection(endPointConnection);
  }

  @Override
  public Link getCurrentLink() {
    return currentLink.get();
  }

  @Override
  public boolean switchTo(Link nextLink, String reason) {
    if(currentLink.get() != nextLink) {
      EndPointLink endPointLink = (EndPointLink) nextLink;
      if(!endPointLink.getEndPointConnection().getState().getName().equals("Established")) {
        endPointLink.getEndPointConnection().scheduleState(new Establishing(endPointLink.getEndPointConnection()));
      }
      currentLink.set(endPointLink);
      return true;
    }
    return false;
  }

  @Override
  public synchronized void changeState(State oldState, State newState) {
    EndPointConnection connection =  newState.getEndPointConnection();
    Link link = routeList.getLink(connection);
    LinkStateChangedEvent event = new LinkStateChangedEvent(link.getLinkId(), oldState.getLinkState(), newState.getLinkState(), Instant.now());
    orchestrator.onLinkStateChanged(event);
  }

}
