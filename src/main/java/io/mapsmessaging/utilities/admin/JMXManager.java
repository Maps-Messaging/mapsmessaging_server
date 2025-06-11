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

package io.mapsmessaging.utilities.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanWrapper;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.admin.Constants;
import lombok.Getter;
import lombok.Setter;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JMXManager {

  @Getter
  @Setter
  private static boolean enableJMX = true;
  @Getter
  @Setter
  private static boolean enableJMXStatistics = true;

  private static final JMXManager instance = new JMXManager();
  private final MBeanServer mbs;
  private final Logger logger = LoggerFactory.getLogger(JMXManager.class);
  private final Map<ObjectName, HealthMonitor> healthMonitorMap;


  public static JMXManager getInstance() {
    return instance;
  }

  public ObjectInstance register(Object obj, List<String> nameList) {

    if(!enableJMX) {
      return null;
    }
    String objectId = JMXHelper.buildObjectName(Constants.JMX_DOMAIN, nameList);
    logger.log(ServerLogMessages.JMX_MANAGER_REGISTER, objectId);
    try {
      ObjectName objectName = new ObjectName(objectId);
      if (obj instanceof HealthMonitor) {
        healthMonitorMap.put(objectName, (HealthMonitor) obj);
      }
      //Test to see if it is an annotated JMX bean, else assume its a normal JMXBean
      Class<?> beanClass = obj.getClass();
      JMXBean jmxBean = beanClass.getAnnotation(JMXBean.class);
      if (jmxBean == null) {
        return mbs.registerMBean(obj, objectName);
      } else {
        return mbs.registerMBean(new JMXBeanWrapper(obj), objectName);
      }
    } catch (Exception e) {
      logger.log(ServerLogMessages.JMX_MANAGER_REGISTER_FAIL, e, objectId);
    }
    return null;
  }

  public void unregister(ObjectInstance instance) {
    if (instance != null && instance.getObjectName() != null) {
      try {
        logger.log(ServerLogMessages.JMX_MANAGER_UNREGISTER, instance.getObjectName().toString());
        healthMonitorMap.remove(instance.getObjectName());
        mbs.unregisterMBean(instance.getObjectName());
      } catch (Exception e) {
        logger.log(ServerLogMessages.JMX_MANAGER_UNREGISTER_FAIL, instance.getObjectName());
      }
    }
  }

  public List<HealthMonitor> getHealthList() {
    return new ArrayList<>(healthMonitorMap.values());
  }

  private JMXManager() {
    mbs = ManagementFactory.getPlatformMBeanServer();
    healthMonitorMap = new ConcurrentHashMap<>();
  }

}
