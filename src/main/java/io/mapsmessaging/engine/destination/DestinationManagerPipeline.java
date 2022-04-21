package io.mapsmessaging.engine.destination;

import static io.mapsmessaging.engine.destination.DestinationImpl.TASK_QUEUE_PRIORITY_SIZE;

import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.audit.AuditEvent;
import io.mapsmessaging.engine.destination.delayed.DelayedMessageManager;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.builders.CommonSubscriptionBuilder;
import io.mapsmessaging.engine.destination.subscription.builders.QueueSubscriptionBuilder;
import io.mapsmessaging.engine.destination.tasks.DelayedMessageProcessor;
import io.mapsmessaging.engine.destination.tasks.ShutdownPhase1Task;
import io.mapsmessaging.engine.destination.tasks.StoreMessageTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class DestinationManagerPipeline {

  private final Map<String, DestinationImpl> destinationList = new LinkedHashMap<>();
  private final Logger logger = LoggerFactory.getLogger(DestinationManagerPipeline.class);
  private final DestinationPathManager rootPath;
  private final Map<String, DestinationPathManager> properties;
  private final List<DestinationManagerListener> destinationManagerListeners;

  DestinationManagerPipeline(DestinationPathManager rootPath, Map<String, DestinationPathManager> properties, List<DestinationManagerListener> destinationManagerListeners){
    this.rootPath = rootPath;
    this.properties = properties;
    this.destinationManagerListeners = destinationManagerListeners;
  }

  public synchronized void put(DestinationImpl destinationImpl) {
    destinationList.put(destinationImpl.getFullyQualifiedNamespace(), destinationImpl);
  }

  public synchronized DestinationImpl create(@NonNull @NotNull String name, @NonNull @NotNull DestinationType destinationType) throws IOException {
    DestinationImpl destinationImpl = destinationList.get(name);
    if (destinationImpl == null) {
      UUID destinationUUID = UUID.randomUUID();
      DestinationPathManager pathManager = rootPath;
      String namespace="";
      for (Map.Entry<String, DestinationPathManager> entry : properties.entrySet()) {
        if (name.startsWith(entry.getKey())) {
          if(namespace.length() < entry.getKey().length()){
            pathManager = entry.getValue();
            namespace = entry.getKey();
          }
        }
      }
      if(destinationType.isTemporary()) {
        destinationImpl = new TemporaryDestination(name, pathManager, destinationUUID, destinationType);
      }
      else{
        destinationImpl = new DestinationImpl(name, pathManager, destinationUUID, destinationType);
      }
      logger.log(AuditEvent.DESTINATION_CREATED, destinationImpl.getFullyQualifiedNamespace());

      destinationList.put(destinationImpl.getFullyQualifiedNamespace(), destinationImpl);
    }

    //
    // let the listeners know there is a new destination
    //
    for (DestinationManagerListener listener : destinationManagerListeners) {
      listener.created(destinationImpl);
    }
    logger.log(ServerLogMessages.DESTINATION_MANAGER_CREATED_TOPIC, name);

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

  public synchronized DestinationImpl delete(DestinationImpl destination){
    DestinationImpl delete = destinationList.remove(destination.getFullyQualifiedNamespace());
    StoreMessageTask deleteDestinationTask = new ShutdownPhase1Task(delete, destinationManagerListeners, logger);
    Future<Response> response = destination.submit(deleteDestinationTask, TASK_QUEUE_PRIORITY_SIZE-1);
    long timeout = System.currentTimeMillis() + 10000; // ToDo: make configurable
    while(!response.isDone() && timeout > System.currentTimeMillis()){
      LockSupport.parkNanos(10000000);
    }
    logger.log(AuditEvent.DESTINATION_DELETED, delete.getFullyQualifiedNamespace());
    return delete;
  }

  public synchronized DestinationImpl find(String name) {
    return destinationList.get(name);
  }

  public synchronized int size(){
    return destinationList.size();
  }

  public synchronized void stop() {
    for (DestinationImpl destinationImpl : destinationList.values()) {
      try {
        destinationImpl.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.DESTINATION_MANAGER_STOPPING,e);
      }
    }
  }

  public synchronized void copy(DestinationFilter filter, Map<String, DestinationImpl> response) {
    if(filter == null){
      response.putAll(destinationList);
    }
    else {
      destinationList.forEach((s, destination) -> {
        if (filter.matches(s)) {
          response.put(s, destination);
        }
      });
    }
  }

  public void scan(){
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
