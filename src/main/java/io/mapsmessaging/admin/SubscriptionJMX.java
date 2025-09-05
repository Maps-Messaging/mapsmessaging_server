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
import io.mapsmessaging.engine.destination.subscription.Subscribable;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.utilities.admin.HealthMonitor;
import io.mapsmessaging.utilities.admin.HealthStatus;
import io.mapsmessaging.utilities.admin.HealthStatus.LEVEL;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Subscription JMX Bean")
public class SubscriptionJMX implements HealthMonitor {

  private final ObjectInstance mbean;
  private final DestinationSubscription subscription;

  public SubscriptionJMX(List<String> parent, DestinationSubscription subscription) {
    this.subscription = subscription;
    List<String> typePath = new ArrayList<>(parent);
    typePath.add("acknowledgementType=" + subscription.getAcknowledgementType());
    typePath.add("session=" + subscription.getSessionId());
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  public void delete() throws IOException {
    Subscribable active = subscription.getDestinationImpl().removeSubscription(subscription.getSessionId());
    if (active != null) {
      active.close();
    }
  }

  @JMXBeanOperation(name = "pause", description = "Pauses the subscription from receiving any new messages")
  public void pause() {
    subscription.pause();
  }

  @JMXBeanOperation(name = "resume", description = "Resumes message delivery to this subscription")
  public void resume() {
    subscription.resume();
  }

  @JMXBeanAttribute(name = "isHibernating", description = "Indicates if this subscription is hibernating, meaning no client session is active")
  public boolean isHibernating() {
    return subscription.isHibernating();
  }

  @JMXBeanAttribute(name = "atRest", description = "Returns the last total number of messages that are waiting on this subscription")
  public int getAtRest() {
    return subscription.getDepth();
  }

  @JMXBeanAttribute(name = "inFlight", description = "Returns the last total number of messages that are in movement for this subscription")
  public int getInFlight() {
    return subscription.getInFlight();
  }

  @JMXBeanAttribute(name = "isPaused", description = "Indicates if this subscription is currently paused")
  public boolean isPaused() {
    return subscription.isPaused();
  }

  @JMXBeanAttribute(name = "sent", description = "Returns the last total number messages sent to the client")
  public long getMessagesSent() {
    return subscription.getMessagesSent();
  }

  @JMXBeanAttribute(name = "acknowledged", description = "Returns the last total number of messages that the client has acknowledged")
  public long getMessagesAcked() {
    return subscription.getMessagesAcked();
  }

  @JMXBeanAttribute(name = "rolledBack", description = "Returns the last total number of messages that the client has rolled back for redelivery")
  public long getMessagesRolledback() {
    return subscription.getMessagesRolledBack();
  }

  @Override
  @JMXBeanOperation(name = "checkHealth", description = "Returns the health status for this subscription")
  public HealthStatus checkHealth() {
    return new HealthStatus(subscription.getName(), LEVEL.INFO, "Subscription seems ok", mbean.getObjectName().toString());
  }

}
