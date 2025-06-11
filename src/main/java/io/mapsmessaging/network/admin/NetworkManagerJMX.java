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
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.network.NetworkManager;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import lombok.Getter;

import javax.management.ObjectInstance;
import java.util.List;

@JMXBean(description = "Network Manager JMX Bean, controls all the End Point Managers")
public class NetworkManagerJMX implements HealthMonitor {

  private final NetworkManager networkManager;
  @Getter
  private final List<String> typePath;
  private final ObjectInstance mbean;

  public NetworkManagerJMX(NetworkManager network) {
    networkManager = network;
    typePath = MessageDaemon.getInstance().getTypePath();
    typePath.add("networkType=NetworkController");
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "start", description = "Starts all End Point manager and will start accepting incoming connections")
  public void start() {
    networkManager.startAll();
  }

  @JMXBeanOperation(name = "stop", description = "Stops all the End Point managers")
  public void stop() {
    networkManager.stopAll();
  }

  @JMXBeanOperation(name = "pause", description = "Pauses all the End Point managers")
  public void pause() {
    networkManager.pauseAll();
  }

  @JMXBeanOperation(name = "resume", description = "Resumes all the End Point managers")
  public void resume() {
    networkManager.resumeAll();
  }

  @JMXBeanAttribute(name = "size", description = "The total number of End Points that have been configured")
  public int size() {
    return networkManager.size();
  }
  //</editor-fold>

  @JMXBeanAttribute(name = "checkHealth", description = "Returns the total number of bytes sent")
  public HealthStatus checkHealth() {
    return new HealthStatus("Network Controller", LEVEL.INFO, "Network Controller seems ok", mbean.getObjectName().toString());
  }

}
