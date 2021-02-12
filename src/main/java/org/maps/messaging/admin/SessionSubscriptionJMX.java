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

package org.maps.messaging.admin;


import com.udojava.jmx.wrapper.JMXBean;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.subscription.impl.DestinationSubscription;
import org.maps.utilities.admin.JMXManager;

@JMXBean(description = "Session Subscription JMX Bean")
public class SessionSubscriptionJMX {

  private final ObjectInstance mbean;
  private final DestinationSubscription subscription;

  public SessionSubscriptionJMX(List<String> parent, DestinationImpl destination, DestinationSubscription subscription){
    List<String> local = new ArrayList<>(MessageDaemon.getInstance().getMBean().getTypePath());
    local.add("destination="+destination.getName());
    this.subscription = subscription;
    mbean = JMXManager.getInstance().register(this, local);
  }

  public void close(){
    JMXManager.getInstance().unregister(mbean);
  }



}
