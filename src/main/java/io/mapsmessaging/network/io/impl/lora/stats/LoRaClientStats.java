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

package io.mapsmessaging.network.io.impl.lora.stats;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDatagram;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import io.mapsmessaging.utilities.stats.Stats;
import io.mapsmessaging.utilities.stats.StatsFactory;
import io.mapsmessaging.utilities.stats.StatsType;
import lombok.Getter;
import lombok.Setter;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@JMXBean(description = "LoRa Radio Status Bean")
public class LoRaClientStats {

  private static final String PACKETS = "Packets";
  private static final String RSSI = "RSSI";

  private static final int[] MOVING_AVERAGE = {1, 5, 10, 15};
  private static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

  private final Stats rssiStats;
  private final Stats missedStats;
  private final Stats receivedStats;
  private final int nodeId;

  @Getter
  private long lastPacketId;

  @Getter
  private long lastReadTime;
  @Getter
  @Setter
  private long lastWriteTime;


  private ObjectInstance mbean;
  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("LoRaClientStats\n");
    sb.append("rssiStats: ").append(rssiStats.getUnits()).append(" ").append(rssiStats.getCurrent()).append("\n");
    sb.append("lastPacketId: ").append(lastPacketId).append("\n");
    sb.append("lastReadTime: ").append(lastReadTime).append("\n");
    sb.append("lastWriteTime: ").append(lastWriteTime).append("\n");
    return sb.toString();
  }

  public LoRaClientStats(List<String> parent, int clientId, StatsType type) {
    rssiStats = StatsFactory.create(type, RSSI, RSSI, ACCUMULATOR.ADD, MOVING_AVERAGE, TIME_UNIT);
    missedStats = StatsFactory.create(type, PACKETS, PACKETS, ACCUMULATOR.AVE, MOVING_AVERAGE, TIME_UNIT);
    receivedStats =  StatsFactory.create(type, "Missed", PACKETS, ACCUMULATOR.AVE,  MOVING_AVERAGE, TIME_UNIT);
    lastPacketId = -1;
    nodeId = clientId;
    movingAveragesJMXList = new ArrayList<>();

    if (JMXManager.isEnableJMX()){
      List<String> jmxPath = new ArrayList<>(parent);
      jmxPath.add("name=RadioStatus");
      jmxPath.add("NodeId=" + clientId);
      mbean = JMXManager.getInstance().register(this, jmxPath);

      if (JMXManager.isEnableJMXStatistics()) {
        List<String> rssiPath = new ArrayList<>(jmxPath);
        List<String> missed = new ArrayList<>(jmxPath);
        List<String> received = new ArrayList<>(jmxPath);
        if (rssiStats.supportMovingAverage()) {
          movingAveragesJMXList.add(new LinkedMovingAveragesJMX(rssiPath, (LinkedMovingAverages) rssiStats));
          movingAveragesJMXList.add(new LinkedMovingAveragesJMX(missed, (LinkedMovingAverages) missedStats));
          movingAveragesJMXList.add(new LinkedMovingAveragesJMX(received, (LinkedMovingAverages) receivedStats));
        }
      }
    }
  }

  public void close() {
    for (LinkedMovingAveragesJMX jmx : movingAveragesJMXList) {
      jmx.close();
    }
    if(mbean != null) JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "getNodeId", description = "Returns the nodes ID")
  public long getNodeId() {
    return nodeId;
  }

  @JMXBeanAttribute(name = "getRSSI", description = "Returns the current RSSI for the client")
  public long getRssi() {
    return rssiStats.getCurrent();
  }

  @JMXBeanAttribute(name = "getReceived", description = "Returns the total received events")
  public long getReceived() {
    return receivedStats.getTotal();
  }

  @JMXBeanAttribute(name = "getMissed", description = "Returns the total missed events")
  public long getMissed() {
    return missedStats.getTotal();
  }

  public void update(LoRaDatagram datagram) {
    lastReadTime = System.currentTimeMillis();
    lastWriteTime = System.currentTimeMillis();
    receivedStats.increment();
    rssiStats.add(datagram.getRssi());
    int id = datagram.getId();
    if (lastPacketId != -1 && id != 0) { // Rolled so ignore
      if (id > lastPacketId) {
        id += 256;
      }
      missedStats.add(id - lastPacketId);
    }
    lastPacketId = id + 1L;
  }

  public void update(int id, int rssi) {
    lastReadTime = System.currentTimeMillis();
    lastWriteTime = System.currentTimeMillis();
    receivedStats.increment();
    rssiStats.add(rssi);
    if (lastPacketId != -1 && id != 0) { // Rolled so ignore
      if (id > lastPacketId) {
        id += 256;
      }
      missedStats.add(id - lastPacketId);
    }
    lastPacketId = id + 1L;
  }
}
