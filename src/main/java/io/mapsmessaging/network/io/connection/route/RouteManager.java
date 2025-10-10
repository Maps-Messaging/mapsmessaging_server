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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.connection.StateChangeListener;
import io.mapsmessaging.network.io.connection.state.Establishing;
import io.mapsmessaging.network.io.connection.state.Hold;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.network.route.select.*;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static io.mapsmessaging.logging.ServerLogMessages.SWITCH_REQUESTED;

@Getter
public class RouteManager implements LinkSwitcher, StateChangeListener {

  private java.util.concurrent.ScheduledExecutorService metricsUpdater;
  private final Logger logger = LoggerFactory.getLogger(RouteManager.class);
  private final String routeName;
  private final AtomicReference<Link> currentLink;

  private final RouteList routeList;
  private final LinkSelector selector;
  private final SelectionOrchestrator orchestrator;

  public RouteManager(String routeName) {
    this.routeName = routeName;
    currentLink = new AtomicReference<>(null);
    routeList = new RouteList(routeName);
    SelectionPolicy policy = SelectionPolicy.builder()
        .establishmentWarmup(java.time.Duration.ofSeconds(3))
        .hysteresisRatio(0.15)
        .tieBreakEpsilon(0.02)
        .build();

    SelectionOrchestratorConfig config = SelectionOrchestratorConfig.builder()
        .enablePeriodicScan(true)
        .periodicScanInterval(Duration.ofSeconds(2))
        .minInterEventInterval(java.time.Duration.ofMillis(75))
        .build();

    selector = LinkSelector.builder()
        .costFunction(new DefaultCostFunction())
        .costWeights(CostWeights.builder().build())
        .selectionPolicy(policy)
        .linkRepository(routeList)
        .linkSwitcher(this)
        .build();
    orchestrator  = SelectionOrchestrator.start(selector, config);
  }

  public void start() {
    routeList.start();
    // Kick off initial selection so one link gets chosen
    selector.evaluateOnce();

    // Start a light scheduler to refresh metrics / trigger selector
    metricsUpdater = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r ->
        new Thread(r, "route-metrics-updater-" + routeName)
    );

    metricsUpdater.scheduleAtFixedRate(this::updateMetricsAndEvaluate, 2, 2, java.util.concurrent.TimeUnit.SECONDS);
  }

  public void stop() {
    routeList.stop();
    if (metricsUpdater != null) {
      metricsUpdater.shutdownNow();
    }
  }

  public void pause() {
    routeList.pause();
  }

  public void resume() {
    routeList.resume();
  }

  public void addEndPointConnection(EndPointConnection endPointConnection) {
    endPointConnection.setEstablishingState(new Hold(endPointConnection)); // Stops it moving to established
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
        logger.log(SWITCH_REQUESTED, nextLink.getLinkId(), reason);
        endPointLink.getEndPointConnection().scheduleState(new Establishing(endPointLink.getEndPointConnection()));
      }
      if(currentLink.get() != null){
        EndPointLink link = (EndPointLink) currentLink.get();
        if(link.getState().equals(LinkState.CONNECTED)) {
          link.getEndPointConnection().scheduleState(new Hold(link.getEndPointConnection()));
        }
      }
      currentLink.set(endPointLink);
      return true;
    }
    return false;
  }

  @Override
  public synchronized void changeState(State oldState, State newState) {
    if(oldState == null)oldState = newState;
    EndPointConnection connection =  newState.getEndPointConnection();
    Link link = routeList.getLink(connection);
    LinkStateChangedEvent event = new LinkStateChangedEvent(link.getLinkId(), oldState.getLinkState(), newState.getLinkState(), Instant.now());
    orchestrator.onLinkStateChanged(event);
  }

  private void updateMetricsAndEvaluate() {
    try {
      for (Link link : routeList.getAllLinks()) {
        link.getMetrics();
      }
      selector.evaluateOnce();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
