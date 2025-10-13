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

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.config.DestinationManagerConfig;
import io.mapsmessaging.dto.rest.config.destination.DestinationConfigDTO;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.engine.resources.MessageExpiryHandler;
import io.mapsmessaging.engine.resources.Resource;
import io.mapsmessaging.engine.resources.ResourceFactory;
import io.mapsmessaging.engine.resources.ResourceProperties;
import io.mapsmessaging.engine.system.SystemTopic;
import io.mapsmessaging.engine.utils.FilePathHelper;
import io.mapsmessaging.license.FeatureManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.Agent;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DestinationManager implements DestinationFactory, Agent {

  private static final String[] QUEUE = {"/queue", "queue"};
  private static final String TEMPORARY_QUEUE = "/dynamic/temporary/queue";
  private static final String TEMPORARY_TOPIC = "/dynamic/temporary/topic";

  private final Map<String, DestinationConfigDTO> properties;
  private final DestinationUpdateManager destinationManagerListeners;
  private final Logger logger;
  private final DestinationConfigDTO rootPath;
  private final DestinationManagerPipeline[] creatorPipelines;

  private final boolean topicsSupported;
  private final boolean queuesSupported;
  private final boolean schemaSupported;
  private final boolean tempTopicsSupported;
  private final boolean tempQueuesSupported;
  private final int maxQueues;
  private final int maxTopics;

  public DestinationManager(FeatureManager featureManager) {
    logger = LoggerFactory.getLogger(DestinationManager.class);
    topicsSupported = featureManager.isEnabled("engine.topicSupport");
    queuesSupported = featureManager.isEnabled("engine.queueSupport");
    tempTopicsSupported = featureManager.isEnabled("engine.tempTopicSupport");
    tempQueuesSupported = featureManager.isEnabled("engine.tempQueueSupport");
    schemaSupported = featureManager.isEnabled("engine.schemaSupport");
    maxQueues = featureManager.getMaxValue("engine.maxQueues");
    maxTopics = featureManager.getMaxValue("engine.maxTopics");
    properties = new LinkedHashMap<>();
    DestinationManagerConfig config = DestinationManagerConfig.getInstance();
    DestinationConfigDTO rootPathLookup = null;
    for(DestinationConfigDTO destinationConfig:config.getData()){
      properties.put(destinationConfig.getNamespaceMapping(), destinationConfig);
      if (destinationConfig.getNamespaceMapping().equals("/")) {
        rootPathLookup = destinationConfig;
      }
    }
    destinationManagerListeners = new DestinationUpdateManager();
    rootPath = rootPathLookup;
    creatorPipelines = new DestinationManagerPipeline[Runtime.getRuntime().availableProcessors() * 2];
    Arrays.setAll(creatorPipelines, x -> new DestinationManagerPipeline(rootPath, properties, destinationManagerListeners));
  }

  int getIndex(String name) {
    int hash = name.hashCode() % creatorPipelines.length;
    return Math.abs(hash);
  }

  public void addSystemTopic(SystemTopic systemTopic) {
    logger.log(ServerLogMessages.DESTINATION_MANAGER_ADD_SYSTEM_TOPIC, systemTopic.getFullyQualifiedNamespace());
    creatorPipelines[getIndex(systemTopic.getFullyQualifiedNamespace())].put(systemTopic);
  }

  @Override
  public CompletableFuture<DestinationImpl> find(String name) {
    return creatorPipelines[getIndex(name)].find(name);
  }

  @Override
  public CompletableFuture<DestinationImpl> findOrCreate(String name) {
    return findOrCreate(name, DestinationType.TOPIC);
  }

  @SneakyThrows
  @Override
  public CompletableFuture<DestinationImpl> findOrCreate(String name, DestinationType destinationType) {
    DestinationImpl destination = find(name).get();
    if (destination != null) {
      CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
      future.complete(destination);
      return future;
    } else {
      return create(name, destinationType);
    }
  }

  @Override
  public CompletableFuture<DestinationImpl> create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
    if (name.startsWith("$SYS")) {
      // can not create these
      logger.log(ServerLogMessages.DESTINATION_MANAGER_USER_SYSTEM_TOPIC, name);
      return null;
    }
    if(destinationType == DestinationType.TOPIC && !topicsSupported ||
        destinationType == DestinationType.QUEUE && !queuesSupported ||
        destinationType == DestinationType.TEMPORARY_QUEUE && !tempQueuesSupported ||
        destinationType == DestinationType.TEMPORARY_TOPIC && !tempTopicsSupported ||
        destinationType == DestinationType.SCHEMA && !schemaSupported
    ){
      logger.log(ServerLogMessages.DESTINATION_MANAGER_NOT_LICESNSED, destinationType.getName());
      return null;
    }
    int max = (destinationType == DestinationType.TOPIC || destinationType == DestinationType.TEMPORARY_TOPIC )? maxTopics : maxQueues;
    int existing = count(destinationType);
    if(max > 0 && existing > max) {
      logger.log(ServerLogMessages.DESTINATION_MANAGER_EXCEEDED_LICESNSE, destinationType.getName(), existing, max);
      return null;
    }
    return creatorPipelines[getIndex(name)].create(name, destinationType);
  }

  @Override
  public CompletableFuture<DestinationImpl> delete(DestinationImpl destinationImpl) {
    if (!destinationImpl.getFullyQualifiedNamespace().startsWith("$SYS")) {
      return creatorPipelines[getIndex(destinationImpl.getFullyQualifiedNamespace())].delete(destinationImpl);
    }
    return null;
  }

  @SneakyThrows
  @Override
  public Map<String, DestinationImpl> get(DestinationFilter filter) {
    Map<String, DestinationImpl> response = new LinkedHashMap<>();
    for (DestinationManagerPipeline pipeline : creatorPipelines) {
      pipeline.copy(filter, response).get();
    }
    return response;
  }

  @SneakyThrows
  public int size() {
    int size = 0;
    for (DestinationManagerPipeline pipeline : creatorPipelines) {
      size += pipeline.size().get();
    }
    return size;
  }

  @SneakyThrows
  public int count(DestinationType type) {
    int size = 0;
    for (DestinationManagerPipeline pipeline : creatorPipelines) {
      size += pipeline.count(type).get();
    }
    return size;
  }


  public void initialise() {
    logger.log(ServerLogMessages.DESTINATION_MANAGER_STARTING);
    for (Map.Entry<String, DestinationConfigDTO> entry : properties.entrySet()) {
      DestinationConfigDTO mapManager = entry.getValue();
      DestinationLocator destinationLocator = new DestinationLocator(mapManager, mapManager.getTrailingPath());
      destinationLocator.parse();
      processFileList(destinationLocator.getValid(), mapManager);
    }
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("Running Pipelines:"+creatorPipelines.length);
    status.setStatus(Status.OK);
    return status;
  }


  @Override
  public String getName() {
    return "Destination Manager";
  }

  @Override
  public String getDescription() {
    return "Manages life cycle of the destinations and manages access to them";
  }

  public void start() {
    initialise();
    logger.log(ServerLogMessages.DESTINATION_MANAGER_STARTING);
    for (DestinationManagerPipeline pipeline : creatorPipelines) {
      pipeline.start();
    }
  }

  public void stop() {
    logger.log(ServerLogMessages.DESTINATION_MANAGER_STOPPING);
    List<CompletableFuture<Boolean>> futures = new ArrayList<>();
    for (DestinationManagerPipeline pipeline : creatorPipelines) {
      futures.add(pipeline.stop());
    }
    CompletableFuture<Void>[] cfs = futures.toArray(new CompletableFuture[0]);
    try {
      CompletableFuture.allOf(cfs).thenApply(ignored -> futures.stream()
              .map(CompletableFuture::join)
              .collect(Collectors.toList())
          ).get(60, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException e) {
        Thread.currentThread().interrupt();
        // We are being interrupted while shutting down. just exit
      }
    }

  public void addListener(DestinationManagerListener listener) {
    destinationManagerListeners.add(listener);
  }

  public boolean removeListener(DestinationManagerListener listener) {
    return destinationManagerListeners.remove(listener);
  }

  public List<DestinationManagerListener> getListeners() {
    return destinationManagerListeners.get();
  }

  private void processFileList(List<File> directories, DestinationConfigDTO pathManager) {
    if (directories != null) {
      ResourceLoaderManagement resourceLoaderManagement = new ResourceLoaderManagement(directories, pathManager);
      resourceLoaderManagement.start();
    }
  }

  private void parseDirectoryPath(File directory, DestinationConfigDTO pathManager) {
    if (directory.isDirectory()) {
      try {
        DestinationImpl destinationImpl = scanDirectory(directory, pathManager);
        if (destinationImpl instanceof TemporaryDestination) {
          // Delete all temporary destinations on restart
          logger.log(ServerLogMessages.DESTINATION_MANAGER_DELETING_TEMPORARY_DESTINATION, destinationImpl.getFullyQualifiedNamespace());
          destinationImpl.delete();
        } else {
          creatorPipelines[getIndex(destinationImpl.getFullyQualifiedNamespace())].put(destinationImpl);
          logger.log(ServerLogMessages.DESTINATION_MANAGER_STARTED_TOPIC, destinationImpl.getFullyQualifiedNamespace());
        }
      } catch (IOException error) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_EXCEPTION_ON_START, error);
      }
    }
  }

  private DestinationImpl scanDirectory(File directory, DestinationConfigDTO pathManager) throws IOException {
    MessageExpiryHandler messageExpiryHandler = new MessageExpiryHandler();
    ResourceProperties scannedProperties = ResourceFactory.getInstance().scanForProperties(directory);
    Resource resource = null;
    if (scannedProperties != null) {
      resource = ResourceFactory.getInstance().scan(messageExpiryHandler, directory, pathManager, scannedProperties);

    }
    if (resource == null) {
      throw new IOException("Invalid resource found");
    }
    String name = scannedProperties.getResourceName();
    if(name.isEmpty()){
      throw new IOException("Invalid name");
    }
    String directoryPath = FilePathHelper.cleanPath(directory.toString() + File.separator);
    DestinationType destinationType = DestinationType.TOPIC;
    DestinationImpl response;
    if (name.toLowerCase().startsWith(TEMPORARY_TOPIC)) {
      destinationType = DestinationType.TEMPORARY_TOPIC;
      response = new TemporaryDestination(name, directoryPath, resource, destinationType, pathManager.getMessageOverride());
    } else if (name.toLowerCase().startsWith(TEMPORARY_QUEUE)) {
      destinationType = DestinationType.TEMPORARY_QUEUE;
      response = new TemporaryDestination(name, directoryPath, resource, destinationType, pathManager.getMessageOverride());
    } else {
      if (name.toLowerCase().startsWith(QUEUE[0]) || name.toLowerCase().startsWith(QUEUE[1])) {
        destinationType = DestinationType.QUEUE;
      }
      response = new DestinationImpl(name, directoryPath, resource, destinationType, pathManager.getMessageOverride());
    }
    messageExpiryHandler.setDestination(response);
    return response;
  }

  public List<String> getAll() {
    Map<String, DestinationImpl> response = new LinkedHashMap<>();
    for(DestinationManagerPipeline pipeline:creatorPipelines){
      pipeline.copy(name -> true, response);
    }
    return new ArrayList<>(response.keySet());
  }

  public long getStorageSize() {
    long count = 0;
    for(DestinationManagerPipeline pipeline:creatorPipelines){
      count += pipeline.getStorageSize();
    }

    return count;
  }

  public class ResourceLoaderManagement {

    private final Queue<File> fileList;
    private final DestinationConfigDTO pathManager;
    private final int initialSize;

    public ResourceLoaderManagement(List<File> list, DestinationConfigDTO pathManager) {
      this.fileList = new ConcurrentLinkedQueue<>(list);
      this.pathManager = pathManager;
      initialSize = list.size();
    }

    public void start() {
      if (fileList.isEmpty()) {
        return;
      }
      // We need to ensure the underlying storage component has loaded and warmed up
      File file = fileList.poll();
      if (file != null) {
        parseDirectoryPath(file, pathManager);
      }

      long report = System.currentTimeMillis() + 1000;
      ResourceLoader[] loaders = new ResourceLoader[Runtime.getRuntime().availableProcessors() * 2];
      for (int x = 0; x < loaders.length; x++) {
        loaders[x] = new ResourceLoader(fileList, pathManager);
        loaders[x].start();
      }
      while (!process(loaders)) {
        if (report <= System.currentTimeMillis()) {
          report = System.currentTimeMillis() + 1000;
          logger.log(ServerLogMessages.DESTINATION_MANAGER_RELOADED, (initialSize - fileList.size()), initialSize);
        }
      }
    }
    private boolean process(ResourceLoader[] loaders) {
      boolean complete = true;
      for (ResourceLoader loader : loaders) {
        if (!loader.complete.get()) {
          complete = false;
          try {
            loader.join(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (Thread.currentThread().isInterrupted()) {
              logger.log(ServerLogMessages.DESTINATION_MANAGER_RELOAD_INTERRUPTED);
              return complete;
            }
          }
        }
      }
      return complete;
    }
  }

  public class ResourceLoader extends Thread {

    private final Queue<File> fileList;
    private final DestinationConfigDTO pathManager;
    private final AtomicBoolean complete;

    public ResourceLoader(Queue<File> fileList, DestinationConfigDTO pathManager) {
      this.fileList = fileList;
      this.pathManager = pathManager;
      complete = new AtomicBoolean(false);
      setName("Resource Loader Thread");
    }

    @Override
    public void run() {
      File file = fileList.poll();
      while (file != null) {
        parseDirectoryPath(file, pathManager);
        file = fileList.poll();
      }
      complete.set(true);
    }
  }
}
