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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointConnectionHostJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.connection.state.*;
import io.mapsmessaging.network.io.connection.state.Shutdown;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.utilities.stats.StatsFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.network.io.connection.Constants.SCHEDULE_TIME;

public class EndPointConnection extends EndPointServerStatus {
  private final AtomicBoolean running;
  private final AtomicBoolean paused;
  private Future<?> futureTask;
  private final EndPointConnectionHostJMX manager;

  @Getter
  private final Logger logger;
  @Getter
  private final EndPointConnectionServerConfig properties;
  @Getter
  private final EndPointConnectionFactory endPointConnectionFactory;
  @Getter
  private final SelectorLoadManager selectorLoadManager;
  @Getter
  private State state;

  @Getter
  @Setter
  private EndPoint endPoint;
  @Getter
  @Setter
  private Protocol connection;

  public EndPointConnection(
      EndPointURL url, EndPointConnectionServerConfig properties,
      EndPointConnectionFactory connectionFactory, SelectorLoadManager selectorLoadManager, EndPointConnectionHostJMX manager) {
    super(url, StatsFactory.getDefaultType());
    this.properties = properties;
    this.manager = manager;
    this.selectorLoadManager = selectorLoadManager;
    this.endPointConnectionFactory = connectionFactory;

    running = new AtomicBoolean(false);
    paused = new AtomicBoolean(false);
    logger = LoggerFactory.getLogger("EndPointConnectionStateManager_" + url.toString() + "_" + properties.getProtocols());
    if (manager != null) {
      manager.addConnection(this);
    }
    logger.log(ServerLogMessages.END_POINT_CONNECTION_INITIALISED);
  }

  public void close() {
    if (futureTask != null && !futureTask.isDone()) {
      futureTask.cancel(false);
    }
    running.set(false);
    if (manager != null) {
      manager.delConnection(this);
    }
    if (endPoint != null) {
      try {
        endPoint.close();
      } catch (IOException ioException) {
        // we are closing the connection here, typically a shutdown
      }
    }
    logger.log(ServerLogMessages.END_POINT_CONNECTION_CLOSED);
  }

  @Override
  public EndPointServerConfigDTO getConfig() {
    return properties;
  }

  public String getConfigName() {
    return properties.getName();
  }
  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    State stateChange;
    if (state instanceof Connecting) {
      stateChange = new Connected(this);
    } else {
      endPoint.close();
      stateChange = new Disconnected(this);
    }
    scheduleState(stateChange);
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    // If the end point closes and we are not running then just let it go
    if (running.get()) {
      scheduleState(new Delayed(this));
    }
  }

  public boolean isStarted(){
    return running.get();
  }

  public void start() {
    if (!running.get()) {
      setRunState(true, new Disconnected(this));
      logger.log(ServerLogMessages.END_POINT_CONNECTION_STARTING);
    }
  }

  public void stop() {
    if(running.get()){
      setRunState(false, new Shutdown(this));
      logger.log(ServerLogMessages.END_POINT_CONNECTION_STOPPING);
    }
  }

  public void pause() {
    if(paused.get()){
      paused.set(true);
    }
  }

  public void resume() {
    if (paused.get()) {
      paused.set(false);
    }
  }

  public List<String> getJMXPath() {
    if (manager == null) {
      return new ArrayList<>();
    }
    return manager.getTypePath();
  }

  private synchronized void setRunState(boolean start, State state) {
    if (running.getAndSet(start) != start) {
      running.set(start);
      scheduleState(state);
    }
  }

  public synchronized void scheduleState(State state) {
    scheduleState(state, SCHEDULE_TIME);
  }

  public synchronized void scheduleState(State newState, long time) {
    if (futureTask != null && !futureTask.isDone()) {
      futureTask.cancel(false);
    }
    if (state != null) {
      logger.log(ServerLogMessages.END_POINT_CONNECTION_STATE_CHANGED, url, properties.getProtocols(), state.getName(), newState.getName());
    }
    setState(newState);
    futureTask = SimpleTaskScheduler.getInstance().schedule(newState, time, TimeUnit.MILLISECONDS);
  }

  private void setState(State state) {
    this.state = state;
  }

  public boolean isPaused() {
    return paused.get();
  }
}
