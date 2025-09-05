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

package io.mapsmessaging.engine.system;

import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.mapsmessaging.logging.ServerLogMessages.SYSTEM_TOPIC_MESSAGE_ERROR;

public class SystemTopicManager implements Runnable, ServiceManager, Agent {

  @Getter
  @Setter
  private static boolean enableStatistics = true;

  @Getter
  @Setter
  private static boolean enableAdvancedStats = true;

  private final ServiceLoader<SystemTopic> systemTopics;
  private final List<SystemTopic> completeList;
  private final DestinationManager destinationManager;

  private Future<?> scheduledFuture;
  private final Logger logger = LoggerFactory.getLogger(SystemTopicManager.class);

  public SystemTopicManager(DestinationManager destinationManager){
    systemTopics = ServiceLoader.load(SystemTopic.class);
    completeList = new ArrayList<>();
    this.destinationManager = destinationManager;
  }

  @Override
  public void run() {
    for (SystemTopic systemTopic : completeList) {
      if (systemTopic.hasUpdates()) {
        try {
          systemTopic.sendUpdate();
        } catch (Throwable e) {
          logger.log(SYSTEM_TOPIC_MESSAGE_ERROR, systemTopic.getClass().getSimpleName(), e);
        }
      }
    }
  }

  @Override
  public String getName() {
    return "System Topic Manager";
  }

  @Override
  public String getDescription() {
    return "System topic life cycle manager";
  }

  @Override
  public void start() {
    if (enableStatistics) {
      for (SystemTopic systemTopic : systemTopics) {
        if (!systemTopic.isAdvanced() || enableAdvancedStats) {
          systemTopic.start();
          destinationManager.addSystemTopic(systemTopic);
          String[] aliases = systemTopic.aliases();
          completeList.add(systemTopic);
          createAliases(aliases, systemTopic);
          addChildren(systemTopic.getChildren());
        }
      }
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(this, 1, 10, TimeUnit.SECONDS);
    }
  }

  private void createAliases(String[] aliases, SystemTopic systemTopic){
    for (String alias : aliases) {
      try {
        SystemTopicAlias aliasTopic = new SystemTopicAlias(alias, systemTopic);
        destinationManager.addSystemTopic(aliasTopic);
      } catch (IOException e) {
        // We can ignore this exception, it is an artifact of the path
      }
    }
  }

  private void addChildren(List<SystemTopic> children){
    if (children != null) {
      for (SystemTopic child : children) {
        destinationManager.addSystemTopic(child);
        completeList.add(child);
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
    systemTopics.forEach(service::add);
    return service.listIterator();
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if (enableStatistics) {
      status.setStatus(Status.OK);
      if(enableAdvancedStats){
        status.setComment("Advanced statistics being recorded");
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

}
