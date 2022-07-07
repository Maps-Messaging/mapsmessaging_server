/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
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
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.resources.ResourceStatistics;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;
import io.mapsmessaging.utilities.admin.LinkedMovingAveragesJMX;
import io.mapsmessaging.utilities.admin.TaskQueueJMX;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.threads.tasks.TaskScheduler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.management.ObjectInstance;

@JMXBean(description = "Destination JMX Bean")
public class DestinationJMX implements HealthMonitor {

  private final DestinationImpl destinationImpl;
  private final ObjectInstance mbean;
  private final List<String> typePath;
  private final List<LinkedMovingAveragesJMX> movingAveragesJMXList;
  private final TaskQueueJMX publishTaskQueueJMX;
  private final TaskQueueJMX subscriptionTaskQueueJMX;

  public DestinationJMX(DestinationImpl destinationImpl, TaskScheduler resource, TaskScheduler subscription) {
    this.destinationImpl = destinationImpl;
    typePath = new ArrayList<>(MessageDaemon.getInstance().getMBean().getTypePath());
    typePath.add("destinationType=" + destinationImpl.getResourceType().getName());
    parseName(destinationImpl.getFullyQualifiedNamespace());
    mbean = JMXManager.getInstance().register(this, typePath);
    movingAveragesJMXList = new ArrayList<>();
    if (!destinationImpl.getFullyQualifiedNamespace().startsWith("$SYS")) {
      DestinationStats stats = destinationImpl.getStats();
      for (LinkedMovingAverages linkedMovingAverages : stats.getAverageList()) {
        movingAveragesJMXList.add(new LinkedMovingAveragesJMX(typePath, linkedMovingAverages));
      }
      ResourceStatistics resourceStatistics = destinationImpl.getResourceStatistics();
      List<String> resourceList = new ArrayList<>(typePath);
      if (destinationImpl.isPersistent()) {
        resourceList.add("resource=file");
      } else {
        resourceList.add("resource=memory");
      }
      for (LinkedMovingAverages linkedMovingAverages : resourceStatistics.getAverageList()) {
        movingAveragesJMXList.add(new LinkedMovingAveragesJMX(resourceList, linkedMovingAverages));
      }
    }

    List<String> pubList = new ArrayList<>(typePath);
    pubList.add("name=ResourceScheduler");
    publishTaskQueueJMX = new TaskQueueJMX(resource, pubList);

    List<String> subList = new ArrayList<>(typePath);
    subList.add("name=SubscriptionScheduler");
    subscriptionTaskQueueJMX = new TaskQueueJMX(subscription, subList);
  }

  private void parseName(String name) {
    if (name.startsWith("/") && name.indexOf('/', 1) == -1) {
      typePath.add("destinationName=" + name);
    } else {
      StringTokenizer st = new StringTokenizer(name, "/");
      List<String> parse = new ArrayList<>();
      while (st.hasMoreTokens()) {
        parse.add(st.nextToken());
      }
      for (int x = 0; x < parse.size() - 1; x++) {
        typePath.add("folder_" + x + "=" + parse.get(x));
      }
      typePath.add("destinationName=" + parse.get(parse.size() - 1));
    }
  }

  public List<String> getTypePath() {
    return typePath;
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
    for (LinkedMovingAveragesJMX movingAveragesJMX : movingAveragesJMXList) {
      movingAveragesJMX.close();
    }
    subscriptionTaskQueueJMX.close();
    publishTaskQueueJMX.close();
  }

  @JMXBeanAttribute(name = "delayed", description = "Returns the number of messages stored but not yet visible to subscribers")
  public long getDelayed() {
    return destinationImpl.getDelayedMessages();
  }

  @JMXBeanAttribute(name = "pendingTransaction", description = "Returns the number of messages waiting for commit")
  public long getPendingTransaction() {
    return destinationImpl.getPendingTransactions();
  }

  @JMXBeanAttribute(name = "stored", description = "Returns the total number of message at rest on this destination")
  public long getStored() throws IOException {
    return destinationImpl.getStoredMessages();
  }

  @JMXBeanAttribute(name = "noInterest", description = "Returns the total number of messages that had no subscriptions and where discarded")
  public long getNoInterest() {
    return destinationImpl.getStats().getNoInterestMessageAverages().getTotal();
  }

  @JMXBeanAttribute(name = "subscribed", description = "Returns the total number of messages that have been subscribed via this destination")
  public long getSubscribed() {
    return destinationImpl.getStats().getSubscribedMessageAverages().getTotal();
  }

  @JMXBeanAttribute(name = "published", description = "Returns the total number of messages published to destination")
  public long getPublished() {
    return destinationImpl.getStats().getPublishedMessageAverages().getTotal();
  }

  @JMXBeanAttribute(name = "retrieved", description = "Returns the total number of messages retrieved from the resource")
  public long getRetrieved() {
    return destinationImpl.getStats().getRetrievedMessagesAverages().getTotal();
  }

  @JMXBeanAttribute(name = "delivered", description = "Returns the total number of events that have been delivered to the remote clients")
  public long getDelivered() {
    return destinationImpl.getStats().getDeliveredMessagesAverages().getTotal();
  }

  @JMXBeanAttribute(name = "expired", description = "Returns total number of messages that have expired on this destination")
  public long getExpired() {
    return destinationImpl.getStats().getExpiredMessagesAverages().getTotal();
  }

  @JMXBeanAttribute(name = "transacted", description = "Returns the total number of messages using transactions added to this destination")
  public long getTransacted() {
    return destinationImpl.getStats().getTransactedPublishedMessageAverages().getTotal();
  }

  @JMXBeanAttribute(name = "subscribedClients", description = "Returns the total number of subscriptions on this destination")
  public long getSubscribedClients() {
    return destinationImpl.getStats().getSubscribedClientAverages().getTotal();
  }

  @JMXBeanOperation(name = "delete", description = "Deletes the destination and all resources used by it")
  public boolean delete() {
    MessageDaemon.getInstance().getDestinationManager().delete(destinationImpl);
    return true;
  }

  @Override
  @JMXBeanOperation(name = "checkHealth", description = "Returns the health status for this destination")
  public HealthStatus checkHealth() {
    return new HealthStatus(destinationImpl.getFullyQualifiedNamespace(), LEVEL.INFO, "Destination seems ok", mbean.getObjectName().toString());
  }


}
