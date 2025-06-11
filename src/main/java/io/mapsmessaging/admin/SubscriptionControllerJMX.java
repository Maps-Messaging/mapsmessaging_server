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
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Session JMX Bean")
public class SubscriptionControllerJMX {

  private final ObjectInstance mbean;
  private final SubscriptionController subscription;


  public SubscriptionControllerJMX(SubscriptionController subscription) {
    this.subscription = subscription;
    List<String> local;
    local = new ArrayList<>(MessageDaemon.getInstance().getTypePath());
    local.add("SessionManager=SessionManager");
    local.add("session=" + subscription.getSessionId());
    mbean = JMXManager.getInstance().register(this, local);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }


  @JMXBeanAttribute(name = "isPersistent", description = "Indicates if the session is persisted to a file store or is only in memory")
  public boolean isPersistent() {
    return subscription.isPersistent();
  }

  @JMXBeanAttribute(name = "State", description = "Returns the current state of this session")
  public String getState() {
    if (subscription.isHibernating()) {
      return "Disconnected";
    }
    return "Active";
  }

  @JMXBeanOperation(name = "Close", description = "Closes and clears all state for this session")
  public void closeSession() {
    MessageDaemon.getInstance().getSubSystemManager().getSessionManager().closeSubscriptionController(subscription);
  }
}
