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

package io.mapsmessaging.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JMXBean(description = "Health Monitor JMX Bean")
public class HealthMonitorJMX {

  private static final String[] ITEM_NAMES = new String[]{"healthId", "level", "message", "resource"};
  private static final String[] ITEM_DESCRIPTIONS = new String[]{"Resource ID name", "current status", "description of the current state", "Object Name of the resource"};
  private static final OpenType<?>[] OBJECT_TYPES = new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING};

  private final ObjectInstance mbean;

  private String myStatus;

  public HealthMonitorJMX(List<String> parent) {
    List<String> typePath = new ArrayList<>(parent);
    typePath.add("service=Health");
    myStatus = "";
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanOperation(name = "health", description = "Returns health status of this node")
  public TabularData health() throws OpenDataException {
    CompositeType compositeType = new CompositeType("HealthStatus", "Messaging Servers current health stat", ITEM_NAMES, ITEM_DESCRIPTIONS, OBJECT_TYPES);
    TabularType tabularType = new TabularType("HealthStatus", "Messaging Servers current health state", compositeType, ITEM_NAMES);
    TabularDataSupport tabularDataSupport = new TabularDataSupport(tabularType);

    List<HealthStatus> list = healthList();
    for (HealthStatus healthStatus : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("healthId", healthStatus.getHealthId());
      map.put("level", healthStatus.getLevel());
      map.put("message", healthStatus.getMessage());
      map.put("resource", healthStatus.getResource());
      tabularDataSupport.put(new CompositeDataSupport(compositeType, map));
    }
    return tabularDataSupport;
  }

  @JMXBeanOperation(name = "healthList", description = "Returns the complete list of Health Status objects")
  public List<HealthStatus> healthList() {
    List<HealthMonitor> monitors = JMXManager.getInstance().getHealthList();
    List<HealthStatus> statuses = new ArrayList<>();
    StringBuilder stringBuffer = new StringBuilder();
    for (HealthMonitor monitor : monitors) {
      HealthStatus status = monitor.checkHealth();
      if (!status.getLevel().name().equals(LEVEL.INFO.name())) {
        stringBuffer.append(status.getMessage()).append(",");
      }
      statuses.add(status);
    }
    myStatus = stringBuffer.toString();
    return statuses;
  }

  @JMXBeanAttribute(name = "currentStatus", description = "Returns the current health status")
  public String getCurrentStatus() {
    return myStatus;
  }
}
