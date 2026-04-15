/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.drone;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.state.drone.core.EntityTwin;
import io.mapsmessaging.state.drone.core.TwinObserver;

public class StateListener implements TwinObserver {

  public StateListener() {
    Thread t = new Thread(() -> {
      while (true) {
        System.out.println("StateListener: " + MessageDaemon.getInstance().getSubSystemManager().getTwinManager().getTwinCount());
        for(EntityTwin twin: MessageDaemon.getInstance().getSubSystemManager().getTwinManager().listTwins()){
          System.out.println("Twin: " + twin);
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    });
    t.start();
  }

}
