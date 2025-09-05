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
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import lombok.Getter;

import javax.management.ObjectInstance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "End Point Manager JMX Bean")
public class EndPointManagerJMX implements HealthMonitor {

  private final EndPointManager endPointManager;
  @Getter
  private final List<String> typePath;
  private final ObjectInstance mbean;

  //<editor-fold desc="Life cycle functions">
  public EndPointManagerJMX(List<String> parent, EndPointManager endPoint, EndPointServerConfigDTO nc) {
    endPointManager = endPoint;
    typePath = new ArrayList<>(parent);
    typePath.add("endPointManagerName=" + endPoint.getName());
    mbean = JMXManager.getInstance().register(this, typePath);
    new NetworkConfigJMX(typePath, nc);
  }

  //</editor-fold>

  //<editor-fold desc="JMX Bean operation functions">
  @JMXBeanOperation(name = "Starts the manager", description = "Starts the end point manager and will start accepting incoming connections")
  public void start() throws IOException {
    endPointManager.start();
  }

  @JMXBeanOperation(name = "Close the manager", description = "Stops all new incoming connections and closes all resources")
  public void close() throws IOException {
    endPointManager.close();
  }

  @JMXBeanOperation(name = "Pause the manager", description = "Stops accepting new incoming connections, current connections are not impacted")
  public void pause() throws IOException {
    endPointManager.pause();
  }

  @JMXBeanOperation(name = "Resume the manager", description = "Resumes accepting new incoming connections")
  public void resume() throws IOException {
    endPointManager.resume();
  }

  @JMXBeanOperation(name = "healthStatus", description = "Returns the total number of bytes sent")
  public HealthStatus checkHealth() {
    return new HealthStatus(endPointManager.getName(), LEVEL.INFO, "End Point manager seems ok", mbean.getObjectName().toString());
  }
  //</editor-fold>

  //<editor-fold desc="JMX Bean Attributes functions">
  @JMXBeanAttribute(name = "Protocols", description = "Returns a list of supported protocols on this end point")
  public String getProtocols() {
    return endPointManager.getProtocols();
  }

  @JMXBeanAttribute(name = "Connected", description = "Returns current active number of end points")
  public int getConnectedEndPoints() {
    return endPointManager.getEndPointServer().size();
  }

  @JMXBeanAttribute(name = "Packets Read", description = "Returns the total number of packets read from all end points")
  public long getTotalPacketsRead() {
    return endPointManager.getEndPointServer().getTotalPacketsRead();
  }

  @JMXBeanAttribute(name = "Packets Sent", description = "Returns the total number of packets sent from all end points")
  public long getTotalPacketsSent() {
    return endPointManager.getEndPointServer().getTotalPacketsSent();
  }

  @JMXBeanAttribute(name = "Bytes Read", description = "Returns the total number of bytes read")
  public long getTotalBytesRead() {
    return endPointManager.getEndPointServer().getTotalBytesRead();
  }

  @JMXBeanAttribute(name = "Bytes Sent", description = "Returns the total number of bytes sent")
  public long getTotalBytesSent() {
    return endPointManager.getEndPointServer().getTotalBytesSent();
  }
  //</editor-fold>

}
