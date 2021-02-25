/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.messaging.engine.destination;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.CreditHandler;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.engine.destination.delayed.DelayedMessageManager;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.builders.CommonSubscriptionBuilder;
import org.maps.messaging.engine.destination.subscription.builders.QueueSubscriptionBuilder;
import org.maps.messaging.engine.destination.tasks.DelayedMessageProcessor;
import org.maps.messaging.engine.destination.tasks.ShutdownPhase1Task;
import org.maps.messaging.engine.destination.tasks.StoreMessageTask;
import org.maps.messaging.engine.resources.Resource;
import org.maps.messaging.engine.resources.ResourceFactory;
import org.maps.messaging.engine.system.SystemTopic;
import org.maps.messaging.engine.tasks.Response;
import org.maps.utilities.configuration.ConfigurationManager;
import org.maps.utilities.configuration.ConfigurationProperties;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class DestinationManager implements DestinationFactory {

  private static final String[] QUEUE = {"/queue", "queue"};
  private static final String TEMPORARY_QUEUE = "/dynamic/temporary/queue";
  private static final String TEMPORARY_TOPIC = "/dynamic/temporary/topic";

  private final Map<String, DestinationPathManager> properties;
  private final Map<String, DestinationImpl> destinationList;
  private final List<DestinationManagerListener> destinationManagerListeners;
  private final Logger logger;
  private final String rootPath;

  public DestinationManager(int time) {
    logger = LoggerFactory.getLogger(DestinationManager.class);
    properties = new LinkedHashMap<>();
    ConfigurationProperties list = ConfigurationManager.getInstance().getProperties("DestinationManager");
    String root = ".";

    Object rootConf = list.get("data");

    if(rootConf instanceof ConfigurationProperties){
      ConfigurationProperties rootCfg = (ConfigurationProperties)rootConf;
      DestinationPathManager destinationPathManager = new DestinationPathManager(rootCfg);
      properties.put(destinationPathManager.getNamespace(), destinationPathManager);
      if (destinationPathManager.getNamespace().equals("/")) {
        root = destinationPathManager.getDirectory();
      }
    }
    else if(rootConf instanceof List) {
      for (Object configuration : (List) rootConf) {
        if (configuration instanceof ConfigurationProperties) {
          DestinationPathManager destinationPathManager = new DestinationPathManager((ConfigurationProperties) configuration);
          properties.put(destinationPathManager.getNamespace(), destinationPathManager);
          if (destinationPathManager.getNamespace().equals("/")) {
            root = destinationPathManager.getDirectory();
          }
        } else {
          break;
        }
      }
    }

    rootPath = root;
    destinationManagerListeners = new CopyOnWriteArrayList<>();
    destinationList = new ConcurrentHashMap<>();
    SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new DelayProcessor(), 2000, time, TimeUnit.MILLISECONDS);
  }

  public void addSystemTopic(SystemTopic systemTopic) {
    logger.log(LogMessages.DESTINATION_MANAGER_ADD_SYSTEM_TOPIC, systemTopic.getName());
    destinationList.put(systemTopic.getName(), systemTopic);
  }

  @Override
  public synchronized List<DestinationImpl> getDestinations() {
    return new ArrayList<>(destinationList.values());
  }

  @Override
  public synchronized DestinationImpl find(String name) {
    return destinationList.get(name);
  }

  @Override
  public synchronized DestinationImpl findOrCreate(String name) throws IOException {
    return findOrCreate(name, DestinationType.TOPIC);
  }

  public String getRoot(){
    return "";
  }

  @Override
  public synchronized DestinationImpl findOrCreate(String name, DestinationType destinationType) throws IOException {
    DestinationImpl destinationImpl = find(name);
    if (destinationImpl == null) {
      destinationImpl = create(name, destinationType);
    }
    return destinationImpl;
  }

  @Override
  public synchronized DestinationImpl create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
    if (name.startsWith("$SYS")) {
      // can not create these
      logger.log(LogMessages.DESTINATION_MANAGER_USER_SYSTEM_TOPIC, name);
      return null;
    }
    DestinationImpl destinationImpl = destinationList.get(name);
    if (destinationImpl == null) {
      UUID destinationUUID = UUID.randomUUID();
      String directoryPath = rootPath;
      for (Map.Entry<String, DestinationPathManager> entry : properties.entrySet()) {
        if (name.startsWith(entry.getKey())) {
          directoryPath = entry.getValue().calculateDirectory(name);
          break;
        }
      }
      if(destinationType.isTemporary()) {
        destinationImpl = new TemporaryDestination(name, directoryPath, destinationUUID, destinationType);
      }
      else{
        destinationImpl = new DestinationImpl(name, directoryPath, destinationUUID, destinationType);
      }
      destinationList.put(destinationImpl.getName(), destinationImpl);
    }

    //
    // let the listeners know there is a new destination
    //
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.created(destinationImpl);
    }
    logger.log(LogMessages.DESTINATION_MANAGER_CREATED_TOPIC, name);

    //-------------------------------------------------------------------------------------
    // We have a divergence here, if we have a Queue then we need to start storing messages
    // even if we have no subscriptions. This is different to Topics since we only store
    // messages when we have subscriptions.
    //-------------------------------------------------------------------------------------
    if(destinationType.isQueue()){
      SubscriptionContext context = new SubscriptionContext(name);
      context.setAcknowledgementController(ClientAcknowledgement.INDIVIDUAL);
      context.setCreditHandler(CreditHandler.CLIENT);
      context.setAlias(name);
      context.setAllowOverlap(false);
      context.setReceiveMaximum(0);
      context.setQualityOfService(QualityOfService.AT_LEAST_ONCE);
      context.setSharedName(name);
      CommonSubscriptionBuilder builder = new QueueSubscriptionBuilder(destinationImpl, context);
      builder.construct(null, name);
    }
    return destinationImpl;
  }

  @Override
  public synchronized DestinationImpl delete(DestinationImpl destinationImpl) {
    if (!destinationImpl.getName().startsWith("$SYS")) {
      DestinationImpl delete = destinationList.remove(destinationImpl.getName());
      StoreMessageTask deleteDestinationTask = new ShutdownPhase1Task(delete, destinationManagerListeners, logger);
      FutureTask<Response> response = destinationImpl.submit(deleteDestinationTask);
      long timeout = System.currentTimeMillis() + 2000; // ToDo: make configurable
      while(!response.isDone() && timeout > System.currentTimeMillis()){
        LockSupport.parkNanos(10000000);
      }
      return delete;
    }
    return null;
  }

  @Override
  public Map<String, DestinationImpl> get() {
    return destinationList;
  }

  public int size() {
    return destinationList.size();
  }

  public void start() {
    logger.log(LogMessages.DESTINATION_MANAGER_STARTING);
    for (Map.Entry<String, DestinationPathManager> entry : properties.entrySet()) {
      DestinationPathManager mapManager = entry.getValue();
      DestinationLocator destinationLocator = new DestinationLocator(mapManager.getRootDirectory(), mapManager.getTrailingPath());
      destinationLocator.parse();
      processFileList(destinationLocator.getValid());
    }
  }

  public void stop() {
    logger.log(LogMessages.DESTINATION_MANAGER_STOPPING);
    for (DestinationImpl destinationImpl : destinationList.values()) {
      try {
        destinationImpl.close();
      } catch (IOException e) {
        logger.log(LogMessages.DESTINATION_MANAGER_STOPPING,e);
      }
    }
  }

  public void addListener(DestinationManagerListener listener) {
    destinationManagerListeners.add(listener);
  }

  public void removeListener(DestinationManagerListener listener) {
    destinationManagerListeners.remove(listener);
  }

  public List<DestinationManagerListener> getListeners() {
    return new ArrayList<>(destinationManagerListeners);
  }

  private void processFileList(List<File> directories){
    if (directories != null) {
      long report = System.currentTimeMillis() + 1000;
      int counter = 0;
      for (File directory : directories) {
        parseDirectoryPath(directory.getParent(), directory);
        counter++;
        if(report <= System.currentTimeMillis()){
          report = System.currentTimeMillis() + 1000;
          logger.log(LogMessages.DESTINATION_MANAGER_RELOADED, counter, directories.size());
        }
      }
    }
  }

  private void parseDirectoryPath(String path, File directory) {
    if (directory.isDirectory()) {
      try {
        DestinationImpl destinationImpl = scanDirectory(path, directory);
        if (destinationImpl instanceof TemporaryDestination) {
          // Delete all temporary destinations on restart
          logger.log(LogMessages.DESTINATION_MANAGER_DELETING_TEMPORARY_DESTINATION, destinationImpl.getName());
          destinationImpl.delete();
        } else {
          destinationList.put(destinationImpl.getName(), destinationImpl);
          logger.log(LogMessages.DESTINATION_MANAGER_STARTED_TOPIC, destinationImpl.getName());
        }
      }
      catch(IOException error){
        logger.log(LogMessages.DESTINATION_MANAGER_EXCEPTION_ON_START, error);
      }
    }
  }

  private DestinationImpl scanDirectory(String root, File directory) throws IOException {
    Resource resource = ResourceFactory.getInstance().scan(root, directory);
    if (resource == null) {
      throw new IOException("Invalid resource found");
    }
    String name = resource.getMappedName();
    DestinationType destinationType = DestinationType.TOPIC;

    if(name.toLowerCase().startsWith(TEMPORARY_TOPIC)){
      destinationType = DestinationType.TEMPORARY_TOPIC;
      return new TemporaryDestination(resource, destinationType);
    }

    if(name.toLowerCase().startsWith(TEMPORARY_QUEUE)){
      destinationType = DestinationType.TEMPORARY_QUEUE;
      return new TemporaryDestination(resource, destinationType);
    }

    if(name.toLowerCase().startsWith(QUEUE[0]) || name.toLowerCase().startsWith(QUEUE[1])){
      destinationType = DestinationType.QUEUE;
    }

    return new DestinationImpl(resource, destinationType);
  }

  public class DelayProcessor implements Runnable{
    @Override
    public void run() {
      for(DestinationImpl destination:destinationList.values()) {
        DelayedMessageManager messageProcessor = destination.getDelayedStatus();
        if (messageProcessor != null && !messageProcessor.isEmpty() ){
          List<Long> waiting = messageProcessor.getBucketIds();
          for(Long expiry:waiting){
            if(expiry < System.currentTimeMillis()){
              destination.submit(new DelayedMessageProcessor(destination, destination.subscriptionManager,messageProcessor, expiry));
            }
            else{
              break;
            }
          }
        }
      }
    }
  }
}
