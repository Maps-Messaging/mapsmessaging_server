/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package io.mapsmessaging.engine.system;

import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Future;

public class SystemTopicManager implements Runnable, ServiceManager {

  private final ServiceLoader<SystemTopic> systemTopics;
  private Future<?> scheduledFuture;
  private final List<SystemTopic> completeList;

  public SystemTopicManager(DestinationManager destinationManager) throws IOException {
    systemTopics = ServiceLoader.load(SystemTopic.class);
    completeList = new ArrayList<>();
    for (SystemTopic systemTopic : systemTopics) {
      destinationManager.addSystemTopic(systemTopic);
      String[] aliases = systemTopic.aliases();
      completeList.add(systemTopic);
      for (String alias : aliases) {
        SystemTopicAlias aliasTopic = new SystemTopicAlias(alias, systemTopic);
        destinationManager.addSystemTopic(aliasTopic);
      }
      List<SystemTopic> children = systemTopic.getChildren();
      if(children != null){
        for(SystemTopic child:children){
          destinationManager.addSystemTopic(child);
          completeList.add(child);
        }
      }
    }
   // scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    for (SystemTopic systemTopic : completeList) {
      if (systemTopic.hasUpdates()) {
        try {
          systemTopic.sendUpdate();
        } catch (IOException e) {
          // We can ignore this, since it would be temp on the connection
        }
      }
    }
  }

  public void stop() {
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(SystemTopic systemTopic:systemTopics){
      service.add(systemTopic);
    }
    return service.listIterator();
  }
}
