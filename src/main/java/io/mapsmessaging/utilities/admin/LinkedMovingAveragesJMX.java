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

import io.mapsmessaging.utilities.stats.LinkedMovingAverages;

import javax.management.*;
import java.util.ArrayList;
import java.util.List;

public class LinkedMovingAveragesJMX implements DynamicMBean {

  private final LinkedMovingAverages movingAverages;
  private final MBeanInfo mBeanInfo;
  private final DetailedStatisticsJMX detailedStatisticsJMX;
  private final ObjectInstance objectInstance;

  public LinkedMovingAveragesJMX(List<String> jmxPath, LinkedMovingAverages movingAverages) {
    this.movingAverages = movingAverages;
    MBeanConstructorInfo[] beanConstructorInfos = new MBeanConstructorInfo[1];
    try {
      beanConstructorInfos[0] = new MBeanConstructorInfo(movingAverages.getName(), this.getClass().getConstructor());
    } catch (NoSuchMethodException noSuchMethodException) {
      // We know it will never be thrown, so we can ignore this
    }

    String[] names = movingAverages.getNames();
    MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[names.length + 2];
    attributeInfos[0] = new MBeanAttributeInfo("total", "java.lang.Long", "Total", true, false, false);
    attributeInfos[1] = new MBeanAttributeInfo("units", "java.lang.String", "Name of the unit", true, false, false);
    for (int x = 0; x < names.length; x++) {
      attributeInfos[x + 2] = new MBeanAttributeInfo(names[x], "java.lang.Long", "Moving Average statistics", true, false, false);
    }
    MBeanOperationInfo[] operationInfos = new MBeanOperationInfo[1];
    operationInfos[0] = new MBeanOperationInfo("reset", "Resets the current statistics", new MBeanParameterInfo[0], "void", MBeanOperationInfo.ACTION);
    mBeanInfo = new MBeanInfo(
        LinkedMovingAveragesJMX.class.toString(),
        "Linked Moving Averages",
        attributeInfos,
        beanConstructorInfos,
        operationInfos,
        new MBeanNotificationInfo[0]
    );
    List<String> copy = new ArrayList<>(jmxPath);
    copy.add("Average=" + movingAverages.getName());
    objectInstance = JMXManager.getInstance().register(this, copy);
    detailedStatisticsJMX = new DetailedStatisticsJMX(copy, movingAverages);
  }

  public void close() {
    JMXManager.getInstance().unregister(objectInstance);
    detailedStatisticsJMX.close();
  }

  @Override
  public Object getAttribute(String attribute) {
    if (attribute.equals("total")) {
      return movingAverages.getTotal();
    }
    if (attribute.equals("units")) {
      return movingAverages.getUnits();
    }
    return movingAverages.getAverage(attribute);
  }

  @Override
  public void setAttribute(Attribute attribute) {
    // There are not attributes to set
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    AttributeList list = new AttributeList();
    for (String attribute : attributes) {
      list.add(getAttribute(attribute));
    }
    return list;
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    return new AttributeList();
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) {
    if (actionName.equals("reset")) {
      movingAverages.reset();
    }
    return null;
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return mBeanInfo;
  }
}
