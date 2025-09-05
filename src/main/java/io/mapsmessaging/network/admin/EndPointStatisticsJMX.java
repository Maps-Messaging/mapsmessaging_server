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
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointStatus;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "End Point statistics")
public class EndPointStatisticsJMX {

  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;
  private final EndPoint endPoint;
  protected final ObjectInstance statsBean;

  public EndPointStatisticsJMX(EndPoint endPoint, List<String> typePath) {
    this.endPoint = endPoint;
    List<String> statsPath = new ArrayList<>(typePath);
    statsPath.add("name=Stats");
    statsBean = JMXManager.getInstance().register(this, statsPath);
    movingAveragesJMXList = new ArrayList<>();
    EndPointStatus status = endPoint.getEndPointStatus();
    if (JMXManager.isEnableJMX() &&
        JMXManager.isEnableJMXStatistics() &&
        status.supportsMovingAverages()
    ) {
      registerMovingAverage(status.getReadByteAverages(), statsPath);
      registerMovingAverage(status.getWriteByteAverages(), statsPath);
      registerMovingAverage(status.getBufferOverFlow(), statsPath);
      registerMovingAverage(status.getBufferUnderFlow(), statsPath);
    }
  }

  public void close() {
    JMXManager.getInstance().unregister(statsBean);
    for (LinkedMovingAveragesJMX movingAveragesJMX : movingAveragesJMXList) {
      movingAveragesJMX.close();
    }
  }

  @JMXBeanAttribute(name = "Last Read", description = "Returns the last read time in milliseconds. ")
  public long getLastRead() {
    return endPoint.getLastRead();
  }

  @JMXBeanAttribute(name = "Last Write", description = "Returns the last write time in milliseconds. ")
  public long getLastWrite() {
    return endPoint.getLastWrite();
  }

  @JMXBeanAttribute(name = "Bytes Read", description = "Returns the total number of bytes read on this end point")
  public long getBytesRead() {
    return endPoint.getEndPointStatus().getReadByteAverages().getTotal();
  }

  @JMXBeanAttribute(name = "Bytes sent", description = "Returns the last total number of bytes sent from this end point")
  public long getBytesSent() {
    return endPoint.getEndPointStatus().getWriteByteAverages().getTotal();
  }

  @JMXBeanAttribute(name = "Total Underflow", description = "Returns the number of times that the buffer did not contain the entire protocol packet")
  public long getTotalUnderFlow() {
    return endPoint.getEndPointStatus().getUnderFlowTotal();
  }

  @JMXBeanAttribute(name = "Total Overflow", description = "Returns the total number of times the local buffer was exceeded")
  public long getTotalOverFlow() {
    return endPoint.getEndPointStatus().getOverFlowTotal();
  }

  private void registerMovingAverage(LinkedMovingAverages movingAverages, List<String> path) {
    movingAveragesJMXList.add(new LinkedMovingAveragesJMX(path, movingAverages));
  }

}