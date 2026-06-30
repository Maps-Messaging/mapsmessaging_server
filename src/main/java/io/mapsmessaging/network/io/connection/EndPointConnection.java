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

package io.mapsmessaging.network.io.connection;

import io.mapsmessaging.dto.rest.config.network.EndPointConnectionServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointConnectionHostJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectionFactory;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.connection.state.Connected;
import io.mapsmessaging.network.io.connection.state.Connecting;
import io.mapsmessaging.network.io.connection.state.Delayed;
import io.mapsmessaging.network.io.connection.state.Disconnected;
import io.mapsmessaging.network.io.connection.state.Establishing;
import io.mapsmessaging.network.io.connection.state.Shutdown;
import io.mapsmessaging.network.io.connection.state.State;
import io.mapsmessaging.network.io.connection.state.StateMonitor;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.utilities.stats.StatsFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.mapsmessaging.network.io.connection.Constants.SCHEDULE_TIME;

public class EndPointConnection extends EndPointServerStatus {

  private final AtomicBoolean running;
  private final AtomicBoolean paused;
  private final AtomicLong transitionSequence;
  private final Object stateLock;

  private final EndPointConnectionHostJMX manager;

  @Getter
  private final Logger logger;

  @Getter
  private final EndPointConnectionServerConfigDTO properties;

  @Getter
  private final EndPointConnectionFactory endPointConnectionFactory;

  @Getter
  private final SelectorLoadManager selectorLoadManager;

  private final StateMonitor stateMonitor;
  private final List<StateChangeListener> stateChangeListeners;

  private Future<?> futureTask;

  @Getter
  private volatile State state;

  @Getter
  @Setter
  private volatile EndPoint endPoint;

  @Getter
  @Setter
  private volatile Protocol protocol;

  @Getter
  @Setter
  private State establishingState;

  public EndPointConnection(
      EndPointURL url,
      EndPointConnectionServerConfigDTO properties,
      EndPointConnectionFactory connectionFactory,
      SelectorLoadManager selectorLoadManager,
      EndPointConnectionHostJMX manager) {
    super(url, StatsFactory.getDefaultType());
    this.properties = properties;
    this.manager = manager;
    this.selectorLoadManager = selectorLoadManager;
    this.endPointConnectionFactory = connectionFactory;
    this.stateChangeListeners = new CopyOnWriteArrayList<>();
    this.establishingState = new Establishing(this);
    this.running = new AtomicBoolean(false);
    this.paused = new AtomicBoolean(false);
    this.transitionSequence = new AtomicLong();
    this.stateLock = new Object();
    this.logger = LoggerFactory.getLogger("EndPointConnectionStateManager_" + url + "_" + properties.getProtocols());

    if (manager != null) {
      manager.addConnection(this);
    }

    stateMonitor = new StateMonitor(this);
    logger.log(ServerLogMessages.END_POINT_CONNECTION_INITIALISED);
  }

  public void close() {
    stateMonitor.close();

    synchronized (stateLock) {
      transitionSequence.incrementAndGet();

      if (futureTask != null && !futureTask.isDone()) {
        futureTask.cancel(false);
      }

      futureTask = null;
      running.set(false);
      paused.set(false);
    }

    if (manager != null) {
      manager.delConnection(this);
    }

    EndPoint currentEndPoint = endPoint;
    if (currentEndPoint != null) {
      try {
        currentEndPoint.close();
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
  public void handleNewEndPoint(EndPoint newEndPoint) throws IOException {
    State currentState = state;
    State stateChange;

    if (running.get() && currentState instanceof Connecting) {
      stateChange = new Connected(this);
    } else {
      newEndPoint.close();
      stateChange = new Disconnected(this);
    }

    scheduleState(stateChange);
  }

  @Override
  public void handleCloseEndPoint(EndPoint closedEndPoint) {
    if (running.get() && !paused.get()) {
      scheduleState(new Delayed(this));
    }
  }

  public void addStateChangeListener(StateChangeListener listener) {
    stateChangeListeners.add(listener);
  }

  public void removeStateChangeListener(StateChangeListener listener) {
    stateChangeListeners.remove(listener);
  }

  public boolean isStarted() {
    return running.get();
  }

  public void start() {
    if (running.compareAndSet(false, true)) {
      paused.set(false);
      stateMonitor.start();
      scheduleState(new Disconnected(this));
      logger.log(ServerLogMessages.END_POINT_CONNECTION_STARTING);
    }
  }

  public void stop() {
    if (running.compareAndSet(true, false)) {
      stateMonitor.stop();
      scheduleState(new Shutdown(this));
      logger.log(ServerLogMessages.END_POINT_CONNECTION_STOPPING);
    }
  }

  public void pause() {
    if (paused.compareAndSet(false, true)) {
      stateMonitor.stop();
    }
  }

  public void resume() {
    if (paused.compareAndSet(true, false)) {
      if (running.get()) {
        stateMonitor.start();
        scheduleState(new Delayed(this));
      }
    }
  }

  public List<String> getJMXPath() {
    if (manager == null) {
      return new ArrayList<>();
    }
    return manager.getTypePath();
  }

  public void scheduleState(State newState) {
    scheduleState(newState, SCHEDULE_TIME);
  }

  public void scheduleState(State newState, long time) {
    if (newState == null) {
      return;
    }

    State oldState;
    long scheduledSequence;

    synchronized (stateLock) {
      oldState = state;
      scheduledSequence = transitionSequence.incrementAndGet();

      if (futureTask != null && !futureTask.isDone()) {
        futureTask.cancel(false);
      }

      if (oldState != null) {
        logger.log(
            ServerLogMessages.END_POINT_CONNECTION_STATE_CHANGED,
            url,
            properties.getProtocols(),
            oldState.getName(),
            newState.getName());
      }

      state = newState;
      futureTask = SimpleTaskScheduler.getInstance().schedule(
          commitStateChange(oldState, newState, scheduledSequence),
          time,
          TimeUnit.MILLISECONDS);
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public boolean isPaused() {
    return paused.get();
  }

  public @NotNull Runnable commitStateChange(State oldState, State newState, long scheduledSequence) {
    return () -> {
      if (!isCurrentTransition(scheduledSequence)) {
        return;
      }

      for (StateChangeListener stateChangeListener : stateChangeListeners) {
        stateChangeListener.changeState(oldState, newState);
      }

      if (!isCurrentTransition(scheduledSequence)) {
        return;
      }

      newState.execute();
    };
  }

  private boolean isCurrentTransition(long scheduledSequence) {
    return transitionSequence.get() == scheduledSequence;
  }
}