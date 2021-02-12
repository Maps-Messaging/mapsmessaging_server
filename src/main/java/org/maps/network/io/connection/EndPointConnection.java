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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.admin.EndPointConnectionHostJMX;
import org.maps.network.admin.EndPointConnectionJMX;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectionFactory;
import org.maps.network.io.EndPointServerStatus;
import org.maps.network.io.connection.state.Connected;
import org.maps.network.io.connection.state.Connecting;
import org.maps.network.io.connection.state.Delayed;
import org.maps.network.io.connection.state.Disconnected;
import org.maps.network.io.connection.state.ShutdownState;
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
  //private final EndPointConnectionJMX bean;
  private final AtomicBoolean running;
  private final AtomicBoolean paused;
  private final SelectorLoadManager selectorLoadManager;
  private final List<ConfigurationProperties> destinationMappings;

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
    running = new AtomicBoolean(true);
    paused = new AtomicBoolean(false);
    logger = LoggerFactory.getLogger("EndPointConnectionStateManager_"+url.toString());
    manager.addConnection(this);
  }

  public void close(){
    running.set(false);
    manager.delConnection(this);
    if(endPoint != null){
      try {
        endPoint.close();
      } catch (IOException ioException) {
        ioException.printStackTrace();
      }
    }
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
    if(state instanceof Connecting){
      state.setState(new Connected(this));
    }
    else{
      endPoint.close();
      state.setState(new Disconnected(this));
    }
    scheduleTask();
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    state.setState(new Delayed(this));
    scheduleTask();
  }

  public Logger getLogger() {
    return logger;
  }

  public State getState(){
    return state;
  }

  public void setState(State state){
    this.state = state;
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
    running.set(true);
    state = new Disconnected(this);
    scheduleTask();
  }

  public void stop(){
    running.set(false);
    setState(new ShutdownState(this));
    scheduleTask();
  }

  private void scheduleTask(){
    if(running.get()) {
      SimpleTaskScheduler.getInstance().schedule(state, 10, TimeUnit.MILLISECONDS);
    }
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

}
