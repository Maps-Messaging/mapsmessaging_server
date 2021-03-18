/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.admin;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import com.udojava.jmx.wrapper.JMXBeanOperation;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;

@JMXBean(description = "Message Daemon JMX Bean")
public class MessageDaemonJMX implements HealthMonitor {

  private final List<String> typePath;
  private final MessageDaemon daemon;
  private final ObjectInstance mbean;
  private final HealthMonitorJMX healthMonitor;
  private final MessageDaemonEntryJMX entryJMX;

  public MessageDaemonJMX(MessageDaemon daemon) {
    this.daemon = daemon;
    typePath = new ArrayList<>();
    typePath.add("type=Broker");
    typePath.add("brokerName=" + daemon.getId());
    mbean = JMXManager.getInstance().register(this, typePath);
    healthMonitor = new HealthMonitorJMX(typePath);
    entryJMX = new MessageDaemonEntryJMX(daemon);
  }

  @JMXBeanOperation(name="shutdown",description = "Initiates a server shutdown")
  public void shutdown() {
    daemon.stop(1);
  }

  //<editor-fold desc="Destination based statistics">
  @JMXBeanAttribute(name = "noInterest", description ="Returns total number of messages with no subscription interst received")
  public long getTotalNoInterest() {
    return DestinationStats.getTotalNoInterestMessages();
  }

  @JMXBeanAttribute(name = "published", description ="Returns the total number of messages received")
  public long getTotalPublishedMessages() {
    return DestinationStats.getTotalPublishedMessages();
  }

  @JMXBeanAttribute(name = "subscribed", description ="Returns the total number of messages that match a subscription")
  public long getTotalSubscribedMessages() {
    return DestinationStats.getTotalSubscribedMessages();
  }

  @JMXBeanAttribute(name = "retrieved", description ="Returns the total number of messages retrieved from underlying storage")
  public long getTotalRetrievedMessages() {
    return DestinationStats.getTotalRetrievedMessages();
  }

  @JMXBeanAttribute(name = "expired", description ="Returns the total number of messages that have expired")
  public long getTotalExpiredMessages() {
    return DestinationStats.getTotalExpiredMessages();
  }

  @JMXBeanAttribute(name = "delivered", description ="Returns the total number of messages that have been delivered to a client and acknowledged")
  public long getTotalDeliveredMessages() {
    return DestinationStats.getTotalDeliveredMessages();
  }

  //</editor-fold>

  @JMXBeanAttribute(name = "packets Received", description ="Returns the total number of protocol specific packets received")
  public long getTotalEventsReceived() {
    return ProtocolImpl.getTotalReceived();
  }

  @JMXBeanAttribute(name = "packets Sent", description ="Returns the total number of protocol specific packets sent")
  public long getTotalEventsSent() {
    return ProtocolImpl.getTotalSent();
  }

  @JMXBeanAttribute(name = "Bytes received", description ="Returns the total number of bytes received across all End Points")
  public long getTotalBytesReceived() {
    return EndPoint.totalReadBytes.sum();
  }

  @JMXBeanAttribute(name = "Bytes sent", description ="Returns the total number of bytes sent across all End Points")
  public long getTotalBytesSent() {
    return EndPoint.totalWriteBytes.sum();
  }

  @JMXBeanAttribute(name = "build Date", description ="Returns the build date and time of the server")
  public String getBuildDate() {
    return BuildInfo.getInstance().getBuildDate();
  }

  @JMXBeanAttribute(name = "build Version", description ="Returns the build version of the server")
  public String getBuildVersion() {
    return BuildInfo.getInstance().getBuildVersion();
  }

  public List<String> getTypePath() {
    return typePath;
  }

  public void close() {
    healthMonitor.close();
    entryJMX.close();
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanOperation(name = "createDestination", description ="Creates a new destination instance")
  public boolean create(String name, boolean isTopic) throws IOException {
    DestinationType type = DestinationType.TOPIC;
    if(!isTopic){
      type = DestinationType.QUEUE;
    }
    daemon.getDestinationManager().create(name, type);
    return true;
  }

  @JMXBeanOperation(name = "checkHealth", description ="Returns the last total number of bytes sent from this end point")
  public HealthStatus checkHealth() {
    return new HealthStatus(daemon.getId(), LEVEL.INFO, "Seems to be running ok", mbean.getObjectName().toString());
  }
}
