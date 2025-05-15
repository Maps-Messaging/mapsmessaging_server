/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
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

package io.mapsmessaging.hardware.trigger;

import io.mapsmessaging.dto.rest.config.device.triggers.BaseTriggerConfigDTO;
import io.mapsmessaging.utilities.service.Service;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Trigger implements Service {

  private final List<Runnable> actions;

  protected Trigger(){
    actions = new CopyOnWriteArrayList<>();
  }

  public void addTask(Runnable runnable){
    actions.add(runnable);
  }

  public void removeTask(Runnable runnable){
    actions.remove(runnable);
  }

  protected void runActions(){
    for (Runnable action : actions) {
      action.run();
    }
  }

  public abstract Trigger build(BaseTriggerConfigDTO properties)throws IOException;

  public abstract void start();

  public abstract void stop();

}
