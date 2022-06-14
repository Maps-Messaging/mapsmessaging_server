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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;

public class EngineManager {
  protected SessionManager manager;
  protected SecurityManager previous;

  public EngineManager(MessageDaemon md){
    manager = md.getSessionManager();
  }

  public void setup(){
    previous = manager.getSecurityManager();
    manager.setSecurityManager(new FakeSecurityManager());
  }

  public void reset(){
    manager.setSecurityManager(previous);
  }

  public boolean hasSessions() {
    return manager.hasSessions();
  }

  public void closeSubscriptionController(SubscriptionController controller) {
    manager.closeSubscriptionController(controller);
  }
}
