/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.lora.stats;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.network.io.impl.lora.device.LoRaDatagram;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import io.mapsmessaging.utilities.stats.MovingAverageFactory.ACCUMULATOR;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectInstance;

@JMXBean(description = "LoRa Radio Status Bean")
public class LoRaClientStats {

  private static final String PACKETS = "Packets";
  private static final String RSSI = "RSSI";

  private static final int[] MOVING_AVERAGE={1, 5, 10, 15};
  private static final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

  private final LinkedMovingAverages rssiStats;
  private final LinkedMovingAverages missedStats;
  private final LinkedMovingAverages receivedStats;
  private final ObjectInstance mbean;
  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;
  private final int nodeId;
  private long lastPacketId;

  public LoRaClientStats(List<String> parent, int clientId){
    rssiStats = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.ADD, RSSI, MOVING_AVERAGE, TIME_UNIT, RSSI);
    missedStats = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.AVE, PACKETS,MOVING_AVERAGE, TIME_UNIT, PACKETS);
    receivedStats = MovingAverageFactory.getInstance().createLinked(ACCUMULATOR.AVE, "Missed",MOVING_AVERAGE, TIME_UNIT, PACKETS);
    lastPacketId = -1;
    nodeId = clientId;
    movingAveragesJMXList = new ArrayList<>();

    List<String> jmxPath = new ArrayList<>(parent);
    jmxPath.add("name=RadioStatus");
    jmxPath.add("NodeId="+clientId);
    mbean = JMXManager.getInstance().register(this, jmxPath);

    List<String> rssiPath = new ArrayList<>(jmxPath);
    movingAveragesJMXList.add(new LinkedMovingAveragesJMX(rssiPath, rssiStats));

    List<String> missed = new ArrayList<>(jmxPath);
    movingAveragesJMXList.add(new LinkedMovingAveragesJMX(missed, missedStats));

    List<String> received = new ArrayList<>(jmxPath);
    movingAveragesJMXList.add(new LinkedMovingAveragesJMX(received, receivedStats));
  }

  public void close(){
    for(LinkedMovingAveragesJMX jmx:movingAveragesJMXList){
      jmx.close();
    }
    JMXManager.getInstance().unregister(mbean);
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

  public void update(LoRaDatagram datagram){
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

  public void update(int id, int rssi){
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
