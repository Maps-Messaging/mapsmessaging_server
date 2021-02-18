/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.io.connection;

import static org.maps.network.io.connection.Constants.SCHEDULE_TIME;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointConnectionHostJMX;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.connection.state.Connected;
import org.maps.network.io.connection.state.Connecting;
import org.maps.network.io.connection.state.Delayed;
import org.maps.network.io.connection.state.Disconnected;
import org.maps.network.io.connection.state.Shutdown;
import org.maps.network.io.connection.state.State;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class EndPointConnection extends EndPointServerStatus {

  private final Logger logger;
  private final ConfigurationProperties properties;
  private final EndPointURL url;
  private final EndPointConnectionHostJMX manager;
  private final EndPointConnectionFactory endPointConnectionFactory;
  private final SelectorLoadManager selectorLoadManager;
  private final List<ConfigurationProperties> destinationMappings;

  private final AtomicBoolean running;
  private final AtomicBoolean paused;

  private Future<Runnable> futureTask;
  private EndPoint endPoint;
  private ProtocolImpl connection;
  private State state;

  public EndPointConnection(
      EndPointURL url, ConfigurationProperties properties, List<ConfigurationProperties> destinationMappings,
      EndPointConnectionFactory connectionFactory,SelectorLoadManager selectorLoadManager, EndPointConnectionHostJMX manager){
    super(url);
    this.properties = properties;
    this.url = url;
    this.manager = manager;
    this.destinationMappings = destinationMappings;
    this.selectorLoadManager = selectorLoadManager;
    this.endPointConnectionFactory = connectionFactory;

    running = new AtomicBoolean(false);
    paused = new AtomicBoolean(false);
    logger = LoggerFactory.getLogger("EndPointConnectionStateManager_"+url.toString()+"_"+ properties.getProperty("protocol"));
    manager.addConnection(this);
    logger.log(LogMessages.END_POINT_CONNECTION_INITIALISED);
  }

  public void close(){
    if(futureTask != null && !futureTask.isDone()){
      futureTask.cancel(true);
    }
    running.set(false);
    manager.delConnection(this);
    if(endPoint != null){
      try {
        endPoint.close();
      } catch (IOException ioException) {
        // we are closing the connection here, typically a shutdown
      }
    }
    logger.log(LogMessages.END_POINT_CONNECTION_CLOSED);
  }

  public ConfigurationProperties getProperties() {
    return properties;
  }

  public List<ConfigurationProperties> getDestinationMappings() {
    return destinationMappings;
  }

  public ProtocolImpl getConnection() {
    return connection;
  }

  public void setConnection(ProtocolImpl connection) {
    this.connection = connection;
  }

  public EndPointConnectionFactory getEndPointConnectionFactory() {
    return endPointConnectionFactory;
  }

  public EndPointURL getUrl() {
    return url;
  }

  @Override
  public NetworkConfig getConfig() {
    return new NetworkConfig(properties);
  }

  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    State stateChange;
    if(state instanceof Connecting){
      stateChange = new Connected(this);
    }
    else{
      endPoint.close();
      stateChange = new Disconnected(this);
    }
    scheduleState(stateChange);
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    // If the end point closes and we are not running then just let it go
    if(running.get()) {
      scheduleState(new Delayed(this));
    }
  }

  public Logger getLogger() {
    return logger;
  }

  public State getState(){
    return state;
  }

  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void setEndPoint(EndPoint endPoint) {
    this.endPoint = endPoint;
  }

  public SelectorLoadManager getSelectorLoadManager() {
    return selectorLoadManager;
  }

  public void start(){
    setRunState(true, new Disconnected(this));
    logger.log(LogMessages.END_POINT_CONNECTION_STARTING);
  }

  public void stop(){
    setRunState(false, new Shutdown(this));
    logger.log(LogMessages.END_POINT_CONNECTION_STOPPING);
  }

  public void pause() {
    paused.set(true);
  }

  public void resume() {
    paused.set(false);
  }

  public List<String> getJMXPath() {
    return manager.getTypePath();
  }

  private synchronized void setRunState(boolean start, State state){
    if(running.getAndSet(start) != start) {
      running.set(start);
      scheduleState(state);
    }
  }

  public synchronized void scheduleState(State state){
    scheduleState(state, SCHEDULE_TIME);
  }

  public synchronized void scheduleState(State newState, long time){
    if(futureTask != null && !futureTask.isDone()){
      futureTask.cancel(true);
    }
    if(state != null) {
      logger.log(LogMessages.END_POINT_CONNECTION_STATE_CHANGED, url, properties.getProperty("protocol"), state.getName(), newState.getName());
    }
    setState(newState);
    futureTask = SimpleTaskScheduler.getInstance().schedule(newState, time, TimeUnit.MILLISECONDS);
  }

  private void setState(State state){
    this.state = state;
  }
}
