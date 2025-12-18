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

package io.mapsmessaging.engine.destination;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.builders.CommonSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.QueueSubscriptionBuilder;
import io.mapsmessaging.engine.destination.tasks.ShutdownPhase1Task;
import io.mapsmessaging.engine.destination.tasks.StoreMessageTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.utilities.threads.tasks.SingleConcurrentTaskScheduler;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import static io.mapsmessaging.engine.destination.DestinationImpl.TASK_QUEUE_PRIORITY_SIZE;

public class DestinationManagerPipeline {

  private final Map<String, DestinationImpl> destinationList;
  private final Logger logger = LoggerFactory.getLogger(DestinationManagerPipeline.class);
  private final DestinationConfigDTO rootPath;
  private final Map<String, DestinationConfigDTO> properties;
  private final DestinationUpdateManager destinationManagerListeners;
  private final ExecutorService taskScheduler;


  DestinationManagerPipeline(DestinationConfigDTO rootPath, Map<String, DestinationConfigDTO> properties, DestinationUpdateManager destinationManagerListeners) {
    this.rootPath = rootPath;
    this.properties = properties;
    this.destinationManagerListeners = destinationManagerListeners;
    taskScheduler = new SingleConcurrentTaskScheduler("DestinationManagerPipeline");
    destinationList = new LinkedHashMap<>();
  }

  public synchronized void put(DestinationImpl destinationImpl) {
    destinationList.put(destinationImpl.getFullyQualifiedNamespace(), destinationImpl);
  }

  public CompletableFuture<DestinationImpl> create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) {
    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    Callable<DestinationImpl> task = () -> {
      try {
        DestinationImpl result = destinationList.get(name);
        if (result == null) {
          result = createInternal(name, destinationType);
        }
        future.complete(result);
        return result;
      } catch (IOException e) {
        future.completeExceptionally(e);
      }
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }

  public CompletableFuture<DestinationImpl> delete(@NonNull @NotNull DestinationImpl destination) {
    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    Callable<DestinationImpl> task = () -> {
      DestinationImpl result = deleteInternal(destination);
      future.complete(result);
      return result;
    };
    taskScheduler.submit(task);
    return future;
  }

  public CompletableFuture<DestinationImpl> find(String name) {
    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    Callable<Void> task = () -> {
      future.complete(destinationList.get(name));
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }

  public synchronized CompletableFuture<Integer> size() {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    Callable<Void> task = () -> {
      future.complete(destinationList.size());
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }


  public CompletableFuture<Void> start() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Callable<Void> task = () -> {
      startInternal();
      future.complete(null);
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }


  public CompletableFuture<Boolean> stop() {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    Callable<Boolean> task = () -> {
      try {
        stopInternal();
      } catch (Exception e) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_EXCEPTION_ON_STOP, e);
      }
      future.complete(true);
      return true;
    };
    taskScheduler.submit(task);
    return future;
  }

  public CompletableFuture<Map<String, DestinationImpl>> copy(DestinationFilter filter, Map<String, DestinationImpl> response) {
    CompletableFuture<Map<String, DestinationImpl>> future = new CompletableFuture<>();
    Callable<Void> task = () -> {
      try {
        future.complete(copyInternal(filter, response));
      } catch (Exception e) {
        // todo log this
        future.completeExceptionally(e);
      }
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }

  private DestinationImpl createInternal(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
    DestinationImpl destinationImpl = destinationList.get(name);
    if (destinationImpl == null) {
      UUID destinationUUID = UuidGenerator.getInstance().generate();
      DestinationConfigDTO pathManager = rootPath;
      String namespace = "";
      for (Map.Entry<String, DestinationConfigDTO> entry : properties.entrySet()) {
        if (name.startsWith(entry.getKey()) &&
            namespace.length() < entry.getKey().length()) {
          pathManager = entry.getValue();
          namespace = entry.getKey();
        }
      }
      if (destinationType.isTemporary()) {
        destinationImpl = new TemporaryDestination(name, pathManager, destinationUUID, destinationType);
      } else {
        destinationImpl = new DestinationImpl(name, pathManager, destinationUUID, destinationType);
      }
      logger.log(AuditEvent.DESTINATION_CREATED, destinationImpl.getFullyQualifiedNamespace());

      destinationList.put(destinationImpl.getFullyQualifiedNamespace(), destinationImpl);
    }

    //-------------------------------------------------------------------------------------
    // We have a divergence here, if we have a Queue then we need to start storing messages
    // even if we have no subscriptions. This is different to Topics since we only store
    // messages when we have subscriptions.
    //-------------------------------------------------------------------------------------
    if (destinationType.isQueue()) {
      SubscriptionContext context = new SubscriptionContext(name);
      context.setAcknowledgementController(ClientAcknowledgement.INDIVIDUAL);
      context.setCreditHandler(CreditHandler.CLIENT);
      context.setAlias(name);
      context.setAllowOverlap(false);
      context.setReceiveMaximum(0);
      context.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
      context.setSharedName(name);
      CommonSubscriptionBuilder builder = new QueueSubscriptionBuilder(destinationImpl, context);
      builder.construct(null, name, name, context.getAllocatedId());
    }

    //
    // let the listeners know there is a new destination
    //
    destinationManagerListeners.created(destinationImpl);
    logger.log(ServerLogMessages.DESTINATION_MANAGER_CREATED_TOPIC, name);
    return destinationImpl;
  }

  private DestinationImpl deleteInternal(@NonNull @NotNull DestinationImpl destination) {
    DestinationImpl delete = destinationList.remove(destination.getFullyQualifiedNamespace());
    StoreMessageTask deleteDestinationTask = new ShutdownPhase1Task(delete, destinationManagerListeners, logger);
    Future<Response> response = destination.submit(deleteDestinationTask, TASK_QUEUE_PRIORITY_SIZE - 1);
    long timeout = System.currentTimeMillis() + 10000;
    while (!response.isDone() && timeout > System.currentTimeMillis()) {
      LockSupport.parkNanos(10000000);
    }
    logger.log(AuditEvent.DESTINATION_DELETED, delete.getFullyQualifiedNamespace());
    return delete;
  }

  private void startInternal() {
    for (DestinationImpl destinationImpl : destinationList.values()) {
      try {
        destinationImpl.scanForOrphanedMessages();
      } catch (IOException e) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_STARTING, e);
      }
    }
  }


  private void stopInternal() {
    for (DestinationImpl destinationImpl : destinationList.values()) {
      try {
        destinationImpl.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_STOPPING, e);
      }
    }
  }

  private Map<String, DestinationImpl> copyInternal(DestinationFilter filter, Map<String, DestinationImpl> response) {
    if (filter == null) {
      response.putAll(destinationList);
    } else {
      destinationList.forEach((s, destination) -> {
        if (filter.matches(s)) {
          response.put(s, destination);
        }
      });
    }
    return response;
  }

  public long getStorageSize() {
    long size =0;
    List<DestinationImpl> tmpList = new ArrayList<>(destinationList.values());
    for (DestinationImpl destinationImpl : tmpList) {
      try {
        size += destinationImpl.getStoredMessages();
      } catch (IOException e) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_STOPPING, e);
      }
    }
    return size;
  }

  public synchronized CompletableFuture<Integer> count(DestinationType type)  {
    CompletableFuture<Integer> future = new CompletableFuture<>();
    Callable<Void> task = () -> {
      int count =0;
      for(DestinationImpl destinationImpl : destinationList.values()) {
        if(destinationImpl.getResourceType().equals(type) &&
            !destinationImpl.getFullyQualifiedNamespace().contains("$SYS") &&
            !destinationImpl.getFullyQualifiedNamespace().contains("$SCHEMA") ) {
          count++;
        }
      }
      future.complete(count);
      return null;
    };
    taskScheduler.submit(task);
    return future;
  }

}
