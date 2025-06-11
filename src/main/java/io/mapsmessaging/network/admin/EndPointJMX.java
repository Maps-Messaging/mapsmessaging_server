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
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import lombok.Getter;

import javax.management.ObjectInstance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "End Point JMX Bean")
public class EndPointJMX implements HealthMonitor {

  protected final EndPoint endPoint;
  protected final ObjectInstance mbean;
  @Getter
  protected final List<String> typePath;
  private final EndPointStatisticsJMX statistics;

  //<editor-fold desc="Life cycle functions">
  public EndPointJMX(List<String> parent, EndPoint endPoint) {
    String endPointName = endPoint.getName();
    endPointName = endPointName.replace(":", "_");
    this.endPoint = endPoint;
    typePath = new ArrayList<>(parent);
    typePath.add("endPointName=" + endPointName);
    mbean = register();
    statistics = new EndPointStatisticsJMX(endPoint, typePath);
  }

  protected ObjectInstance register() {
    return JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
    statistics.close();
  }
  //</editor-fold>

  //<editor-fold desc="JMX Bean functions">
  @JMXBeanOperation(name = "close", description = "Closes the low level network End Point")
  public void closeEndPoint() throws IOException {
    endPoint.close();
  }

  @JMXBeanOperation(name = "checkHealth", description = "Returns current health status of this End Point ")
  public HealthStatus checkHealth() {
    return new HealthStatus(endPoint.getName(), LEVEL.INFO, "End Point seems ok", mbean.getObjectName().toString());
  }
  //</editor-fold>

}
