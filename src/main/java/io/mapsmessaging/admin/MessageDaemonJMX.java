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
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.Stats;
import lombok.Getter;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Message Daemon JMX Bean")
public class MessageDaemonJMX implements HealthMonitor {

  @Getter
  private final List<String> typePath;
  private final MessageDaemon daemon;
  private final ObjectInstance mbean;
  private final HealthMonitorJMX healthMonitor;
  private final MessageDaemonEntryJMX entryJMX;
  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;

  public MessageDaemonJMX(MessageDaemon daemon) {
    this.daemon = daemon;
    typePath = new ArrayList<>();
    typePath.add("type=Broker");
    typePath.add("brokerName=" + daemon.getId());
    List<String> resourceList = new ArrayList<>(typePath);
    mbean = JMXManager.getInstance().register(this, typePath);
    healthMonitor = new HealthMonitorJMX(typePath);
    entryJMX = new MessageDaemonEntryJMX(daemon);
    movingAveragesJMXList = new ArrayList<>();
    if (JMXManager.isEnableJMXStatistics()) {
      for (Stats linkedMovingAverages : DestinationImpl.getGlobalStats().getGlobalAverages()) {
        if (linkedMovingAverages.supportMovingAverage()) {
          movingAveragesJMXList.add(
              new LinkedMovingAveragesJMX(resourceList, (LinkedMovingAverages) linkedMovingAverages));
        }
      }
    }
  }

  @JMXBeanOperation(name = "shutdown", description = "Initiates a server shutdown")
  public void shutdown() {
    daemon.stop();
  }

  //<editor-fold desc="Destination based statistics">
  @JMXBeanAttribute(name = "noInterest", description = "Returns total number of messages with no subscription interst received")
  public long getTotalNoInterest() {
    return DestinationImpl.getGlobalStats().getTotalNoInterestMessages();
  }

  @JMXBeanAttribute(name = "published", description = "Returns the total number of messages received")
  public long getTotalPublishedMessages() {
    return  DestinationImpl.getGlobalStats().getTotalPublishedMessages();
  }

  @JMXBeanAttribute(name = "subscribed", description = "Returns the total number of messages that match a subscription")
  public long getTotalSubscribedMessages() {
    return  DestinationImpl.getGlobalStats().getTotalSubscribedMessages();
  }

  @JMXBeanAttribute(name = "retrieved", description = "Returns the total number of messages retrieved from underlying storage")
  public long getTotalRetrievedMessages() {
    return  DestinationImpl.getGlobalStats().getTotalRetrievedMessages();
  }

  @JMXBeanAttribute(name = "expired", description = "Returns the total number of messages that have expired")
  public long getTotalExpiredMessages() {
    return  DestinationImpl.getGlobalStats().getTotalExpiredMessages();
  }

  @JMXBeanAttribute(name = "delivered", description = "Returns the total number of messages that have been delivered to a client and acknowledged")
  public long getTotalDeliveredMessages() {
    return  DestinationImpl.getGlobalStats().getTotalDeliveredMessages();
  }

  //</editor-fold>

  @JMXBeanAttribute(name = "packets Received", description = "Returns the total number of protocol specific packets received")
  public long getTotalEventsReceived() {
    return EndPoint.totalReceived.sum();
  }

  @JMXBeanAttribute(
      name = "packets Sent",
      description = "Returns the total number of protocol specific packets sent")
  public long getTotalEventsSent() {
    return EndPoint.totalSent.sum();
  }

  @JMXBeanAttribute(name = "Bytes received", description = "Returns the total number of bytes received across all End Points")
  public long getTotalBytesReceived() {
    return EndPoint.totalReadBytes.sum();
  }

  @JMXBeanAttribute(name = "Bytes sent", description = "Returns the total number of bytes sent across all End Points")
  public long getTotalBytesSent() {
    return EndPoint.totalWriteBytes.sum();
  }

  @JMXBeanAttribute(name = "build Date", description = "Returns the build date and time of the server")
  public String getBuildDate() {
    return BuildInfo.getBuildDate();
  }

  @JMXBeanAttribute(name = "build Version", description = "Returns the build version of the server")
  public String getBuildVersion() {
    return BuildInfo.getBuildVersion();
  }

  public void close() {
    healthMonitor.close();
    entryJMX.close();
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanOperation(name = "checkHealth", description = "Returns the last total number of bytes sent from this end point")
  public HealthStatus checkHealth() {
    return new HealthStatus(daemon.getId(), LEVEL.INFO, "Seems to be running ok", mbean.getObjectName().toString());
  }
}
