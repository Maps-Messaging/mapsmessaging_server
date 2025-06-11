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

@JMXBean(description = "End Point Connection JMX Bean")
public class EndPointConnectionJMX implements HealthMonitor {

  private final EndPointConnection connection;
  @Getter
  private final List<String> typePath;
  private final ObjectInstance mbean;

  public EndPointConnectionJMX(List<String> parent, EndPointConnection connection) {
    this.connection = connection;
    typePath = new ArrayList<>(parent);
  //  typePath.add("connection=" + connection.getProperties().getProperty("direction"));
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }


  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "pause", description = "Pauses the connection")
  public void pauseConnection() {
    connection.pause();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "resume", description = "Resumes the connection")
  public void resumeConnection() {
    connection.resume();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "stop", description = "Stops the connection")
  public void stopConnection() {
    connection.stop();
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "start", description = "Starts the connection")
  public void startConnection() {
    connection.start();
  }

  @Override
  public HealthStatus checkHealth() {
    return new HealthStatus("OK", LEVEL.INFO, "OK", "Network");
  }
}