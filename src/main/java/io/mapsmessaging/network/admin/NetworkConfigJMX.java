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

import io.mapsmessaging.config.Config;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.*;
import java.util.ArrayList;
import java.util.List;

public class NetworkConfigJMX implements DynamicMBean {

  protected final ObjectInstance mbean;
  protected final List<String> typePath;
  private final EndPointServerConfigDTO networkConfig;
  private final MBeanInfo mBeanInfo;

  public NetworkConfigJMX() {
    mbean = null;
    typePath = null;
    networkConfig = null;
    mBeanInfo = null;
  }

  //<editor-fold desc="Life cycle functions">
  public NetworkConfigJMX(List<String> parent, EndPointServerConfigDTO networkConfig) {
    this.networkConfig = networkConfig;
    typePath = new ArrayList<>(parent);
    typePath.add("config=Config");
    MBeanConstructorInfo[] beanConstructorInfos = new MBeanConstructorInfo[1];
    try {
      beanConstructorInfos[0] = new MBeanConstructorInfo("Config", this.getClass().getConstructor());
    } catch (NoSuchMethodException noSuchMethodException) {
      // We know it will never be thrown so we can ignore this
    }
    List<String> keyList = new ArrayList<>(((Config)networkConfig).toConfigurationProperties().keySet());

    MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[keyList.size()];
    for (int x = 0; x < keyList.size(); x++) {
      String key = keyList.get(x);
      attributeInfos[x] = new MBeanAttributeInfo(key, "java.lang.String", "Configuration Entry", true, false, false);
    }

    mBeanInfo = new MBeanInfo(
        NetworkConfigJMX.class.toString(),
        "End Point configuration",
        attributeInfos,
        beanConstructorInfos,
        null,
        new MBeanNotificationInfo[0]
    );
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @Override
  public Object getAttribute(String attribute) {
    if (attribute.toLowerCase().contains("pass")) {
      return "**********";
    } else {
      return ((Config)networkConfig).toConfigurationProperties().get(attribute);
    }
  }

  @Override
  public void setAttribute(Attribute attribute) throws AttributeNotFoundException {
    throw new AttributeNotFoundException("Attribute not writable " + attribute.getName());
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    AttributeList response = new AttributeList();
    for (String attr : attributes) {
      try {
        response.add(getAttribute(attr));
      } catch (Exception e) {
        response.add(e);
      }
    }
    return response;
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    return new AttributeList();
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) {
    return null;
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return mBeanInfo;
  }
}
