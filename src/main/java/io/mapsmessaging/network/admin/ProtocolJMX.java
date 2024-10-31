/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;

@JMXBean(description = "Protocol JMX Bean, manages a single instance of a protocol")
public class ProtocolJMX implements HealthMonitor {

  private final ProtocolImpl protocol;
  private final ObjectInstance mbean;

  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;

  //<editor-fold desc="Life cycle functions">
  public ProtocolJMX(List<String> parent, ProtocolImpl protocol) {
    this.protocol = protocol;
    List<String> list = new ArrayList<>(parent);
    list.add("Protocol=" + protocol.getName());
    mbean = JMXManager.getInstance().register(this, list);
    movingAveragesJMXList = new ArrayList<>();
    List<String> typePath = new ArrayList<>(list);
    EndPointStatus status = protocol.getEndPoint().getEndPointStatus();
    if (JMXManager.isEnableJMX() &&
        JMXManager.isEnableJMXStatistics() &&
        status.supportsMovingAverages()
    ) {
      registerMovingAverage(status.getReceivedMessages(), typePath);
      registerMovingAverage(status.getSentMessages(), typePath);
    }
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
    for (LinkedMovingAveragesJMX movingAveragesJMX : movingAveragesJMXList) {
      movingAveragesJMX.close();
    }
  }

  private void registerMovingAverage(LinkedMovingAverages movingAverages, List<String> path) {
    movingAveragesJMXList.add(new LinkedMovingAveragesJMX(path, movingAverages));
  }
  //</editor-fold>

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanAttribute(name = "name", description = "Returns the name of the End Point that this protocol instance is bound to")
  public String getEndPointName() {
    return protocol.getEndPoint().getName();
  }

  @JMXBeanAttribute(name = "version", description = "Returns the version of the protocol being used")
  public String getVersion() {
    return protocol.getVersion();
  }

  @JMXBeanAttribute(name = "session", description = "Returns the session identifier")
  public String getSessionId() {
    return protocol.getSessionId();
  }

  @JMXBeanAttribute(name = "received", description = "Returns total number of protocol specific packets received")
  public long getTotalReceivedMessages() {
    return protocol.getEndPoint().getEndPointStatus().getReceivedMessagesTotal();
  }

  @JMXBeanAttribute(name = "sent", description = "Returns total number of protocol specific packets sent")
  public long getTotalSentMessages() {
    return protocol.getEndPoint().getEndPointStatus().getSentMessagesTotal();
  }
  //</editor-fold>

  @JMXBeanOperation(name = "close", description = "Closes the connection with the client")
  public void closeProtocol() throws IOException {
    protocol.close();
  }

  @Override
  @JMXBeanOperation(name = "checkHealth", description = "Returns the current health state")
  public HealthStatus checkHealth() {
    return new HealthStatus(protocol.getName(), LEVEL.WARN, "Protocol seems to have an issue", mbean.getObjectName().toString());
  }

}
