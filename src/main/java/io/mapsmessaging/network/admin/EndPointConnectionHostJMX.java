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

package io.mapsmessaging.network.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import lombok.Getter;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "End Point Connection Host Management JMX Bean")
public class EndPointConnectionHostJMX implements HealthMonitor {

  private final List<EndPointConnection> connections;
  @Getter
  private final List<String> typePath;
  private final ObjectInstance mbean;

  public EndPointConnectionHostJMX(List<String> parent, String host) {
    connections = new ArrayList<>();
    typePath = new ArrayList<>(parent);
    typePath.add("connectionType=OutboundConnections");
    typePath.add("remoteHost=" + host);
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  public void addConnection(EndPointConnection connection) {
    connections.add(connection);
  }

  public void delConnection(EndPointConnection connection) {
    connections.remove(connection);
  }


  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "pauseAll", description = "Pauses all the connections to the specified host")
  public void pauseConnection() {
    for (EndPointConnection connection : connections) {
      connection.pause();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "resumeAll", description = "Resumes all the connections to the specified host")
  public void resumeConnection() {
    for (EndPointConnection connection : connections) {
      connection.resume();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "stopAll", description = "Stops all the connections to the specified host")
  public void stopConnection() {
    for (EndPointConnection connection : connections) {
      connection.stop();
    }
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "startAll", description = "Starts all the connections to the specified host")
  public void startConnection() {
    for (EndPointConnection connection : connections) {
      connection.start();
    }
  }

  @Override
  public HealthStatus checkHealth() {
    return new HealthStatus("OK", LEVEL.INFO, "OK", "Network");
  }
}
