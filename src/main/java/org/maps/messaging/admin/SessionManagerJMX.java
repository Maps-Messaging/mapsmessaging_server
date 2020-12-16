/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.admin;


import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectInstance;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.engine.session.SessionManager;
import org.maps.utilities.admin.JMXManager;

@JMXBean(description = "Session Management JMX Bean")
public class SessionManagerJMX {

  private final SessionManager sessionManager;
  private final ObjectInstance mbean;
  private final List<String> typePath;

  public SessionManagerJMX(SessionManager sessionManager){
    this.sessionManager = sessionManager;
    typePath = new ArrayList<>(MessageDaemon.getInstance().getMBean().getTypePath());
    typePath.add("SessionManager=SessionManager");
    mbean = JMXManager.getInstance().register(this, typePath);
  }

  public void close(){
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "activeSessions", description ="Returns the current number of active sessions")
  public long getActiveSessions() {
    return sessionManager.getConnected();
  }

  @JMXBeanAttribute(name = "disconnectedSessions", description ="Returns the current number of disconnected sessions")
  public long getDisconnectedSessions() {
    return sessionManager.getDisconnected();
  }

  @JMXBeanAttribute(name = "expiredSessions", description ="Returns the total number of sessins that have expired and been closed")
  public long getExpiredSessions() {
    return sessionManager.getTotalExpired();
  }


}
