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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import java.util.List;

public class SessionManagerTest {

  private static final SessionManagerTest instance = new SessionManagerTest();

  public static SessionManagerTest getInstance(){
    return instance;
  }

  private final SessionManager manager;

  SessionManagerTest(){
    manager = MessageDaemon.getInstance().getSubSystemManager().getSessionManager();
  }


  public SubscriptionController getIdleSubscriptions(String idleSession){
    return manager.getIdleSubscriptions(idleSession);
  }

  public List<String> getIdleSessions(){
    return manager.getIdleSessions();
  }

  public boolean hasIdleSessions(){
    return manager.hasIdleSessions();
  }

  public boolean hasSessions() {
    return manager.hasSessions();
  }

  public void closeIdleSession(String idleSession) {
    SubscriptionController sub = manager.getIdleSubscriptions(idleSession);
    if(sub != null){
      manager.closeSubscriptionController(sub);
      sub.close();
    }
  }
}
