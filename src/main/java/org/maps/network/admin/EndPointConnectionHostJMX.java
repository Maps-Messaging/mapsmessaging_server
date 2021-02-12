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

package org.maps.network.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.admin.HealthMonitor;
import org.maps.utilities.admin.HealthStatus;
import org.maps.utilities.admin.HealthStatus.LEVEL;
import org.maps.utilities.admin.JMXManager;

@JMXBean(description = "End Point Connection Host Management JMX Bean")
public class EndPointConnectionHostJMX implements HealthMonitor {
  private final List<EndPointConnection> connections;
  private final List<String> typePath;
  private final ObjectInstance mbean;

  public EndPointConnectionHostJMX(List<String> parent, String host) {
    connections = new ArrayList<>();
    typePath = new ArrayList<>(parent);
    typePath.add("networkType=OutboundConnections");
    typePath.add("remoteHost="+host);
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close(){
    JMXManager.getInstance().unregister(mbean);
  }

  public void addConnection(EndPointConnection connection){
    connections.add(connection);
  }

  public void delConnection(EndPointConnection connection){
    connections.remove(connection);
  }

  public List<String> getTypePath() {
    return typePath;
  }


  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "pauseAll", description ="Pauses all the connections to the specified host")
  public void pauseConnection() throws IOException {
    for(EndPointConnection connection:connections) {
      connection.pause();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "resumeAll", description ="Resumes all the connections to the specified host")
  public void resumeConnection() throws IOException {
    for(EndPointConnection connection:connections) {
      connection.resume();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "stopAll", description ="Stops all the connections to the specified host")
  public void stopConnection() throws IOException {
    for(EndPointConnection connection:connections) {
      connection.stop();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "startAll", description ="Starts all the connections to the specified host")
  public void startConnection() throws IOException {
    for(EndPointConnection connection:connections) {
      connection.start();
    }
  }

  @Override
  public HealthStatus checkHealth() {
    return new HealthStatus("OK", LEVEL.INFO, "OK", "Network");
  }
}
