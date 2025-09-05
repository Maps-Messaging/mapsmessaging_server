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
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.session.SessionManager;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.ArrayList;
import java.util.List;

@JMXBean(description = "Session Management JMX Bean")
public class SessionManagerJMX {

  private final SessionManager sessionManager;
  private final ObjectInstance mbean;

  public SessionManagerJMX(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
    List<String> typePath = new ArrayList<>(MessageDaemon.getInstance().getTypePath());
    typePath.add("SessionManager=SessionManager");
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "activeSessions", description = "Returns the current number of active sessions")
  public long getActiveSessions() {
    return sessionManager.getConnected();
  }

  @JMXBeanAttribute(name = "disconnectedSessions", description = "Returns the current number of disconnected sessions")
  public long getDisconnectedSessions() {
    return sessionManager.getDisconnected();
  }

  @JMXBeanAttribute(name = "expiredSessions", description = "Returns the total number of sessins that have expired and been closed")
  public long getExpiredSessions() {
    return sessionManager.getTotalExpired();
  }


}
