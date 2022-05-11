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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.engine.closure.ClosureTask;
import io.mapsmessaging.engine.closure.ClosureTaskManager;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.utilities.scheduler.SimpleTaskScheduler;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

public class SessionImpl {
  private final Logger logger;
  private final SecurityContext securityContext;
  private final SessionContext context;
  private final Future<?> scheduledFuture;
  private final SubscriptionController subscriptionManager;
  private final DestinationFactory destinationManager;
  private WillTaskImpl willTaskImpl;
  private final ClosureTaskManager closureTaskManager;
  private MessageCallback messageCallback;
  private boolean isClosed;
  private long expiry;
  private final NamespaceMap namespaceMapping;

  //<editor-fold desc="Life cycle API">
  SessionImpl(SessionContext context,
      SecurityContext securityContext,
      DestinationFactory destinationManager,
      SubscriptionController subscriptionManager) {
    logger = LoggerFactory.getLogger(SessionImpl.class);
    this.securityContext = securityContext;
    this.context = context;
    this.subscriptionManager = subscriptionManager;
    this.destinationManager = destinationManager;
    closureTaskManager = new ClosureTaskManager();
    namespaceMapping = new NamespaceMap();
    isClosed = false;
    if (context.getSessionExpiry() == -1) {
      expiry = 24L * 60L * 60L; // One Day
    } else {
      expiry = context.getSessionExpiry();
    }
    //
    // Schedule a keep alive
    //
    if (context.getProtocol().getKeepAlive() != 0) {
      long ka = context.getProtocol().getKeepAlive() + 5000L; // allow 5 seconds more
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new KeepAliveTask(context.getProtocol()), ka, ka, TimeUnit.MILLISECONDS);
      logger.log(ServerLogMessages.SESSION_MANAGER_KEEP_ALIVE_TASK);
    } else {
      scheduledFuture = null;
    }
  }

  SubscriptionController getSubscriptionController() {
    return subscriptionManager;
  }

  void close() {
    logger.log(ServerLogMessages.SESSION_MANAGER_CLOSING_SESSION, context.getId());
    isClosed = true;
    securityContext.logout();
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    closureTaskManager.close();
    namespaceMapping.clear();
  }

  public void resumeState() {
    subscriptionManager.wakeAll(this);
  }

  public SubscribedEventManager resume(DestinationImpl destination) {
    return subscriptionManager.wake(this, destination);
  }

  public void start() {
    subscriptionManager.wake(this);
  }

  public void login() throws IOException {
    securityContext.login();
    ((SessionDestinationManager)destinationManager).setSessionTenantConfig(TenantManagement.build(context.getProtocol(), securityContext));
    // Only do this once the connection has be authenticated
    this.willTaskImpl = createWill(context);
  }

  public SecurityContext getSecurityContext(){
    return securityContext;
  }

  //</editor-fold>

  //<editor-fold desc="Destination Control API">
  @SneakyThrows
  public CompletableFuture<DestinationImpl> findDestination(String destinationName, @Nullable DestinationType destinationType) throws IOException {
    if(isClosed){
      throw new IOException("Session is closed");
    }
    String mapped = namespaceMapping.getMapped(destinationName);
    if(mapped == null){
      mapped = destinationManager.calculateNamespace(destinationName);
      namespaceMapping.addMapped(destinationName, mapped);
    }
    String finalMapped = mapped;

    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    DestinationImpl existing = destinationManager.find(mapped).get();
    if(existing != null){
      future.complete(existing);
    }
    else {
      Callable<DestinationImpl> callable = () -> {
        DestinationImpl created =  null;
        try {
          CompletableFuture<DestinationImpl> creationFuture = destinationManager.create(finalMapped, destinationType);
          created = creationFuture.get();
          future.complete(created);
        } catch (IOException | ExecutionException | InterruptedException e) {
          future.completeExceptionally(e);
        }
        return created;
      };
      future.completeAsync(() -> {
        try {
          return callable.call();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });
    }
    return future;
  }

  public MessageCallback getMessageCallback() {
    return messageCallback;
  }

  public void setMessageCallback(MessageCallback messageCallback) {
    this.messageCallback = messageCallback;
  }

  public CompletableFuture<DestinationImpl>  deleteDestination(DestinationImpl destinationImpl) {
    namespaceMapping.removeByMapped(destinationImpl.getFullyQualifiedNamespace());
    return destinationManager.delete(destinationImpl);
  }
  //</editor-fold>

  //<editor-fold desc="Session state API">
  public String getName() {
    return context.getId();
  }

  public ProtocolImpl getProtocol() {
    return context.getProtocol();
  }

  public boolean isClosed() {
    return isClosed;
  }

  public boolean isRestored() {
    return context.isRestored();
  }

  public void setExpiryTime(long expiry) {
    this.expiry = expiry;
  }

  public long getExpiry() {
    return expiry;
  }

  public int getReceiveMaximum() {
    return context.getReceiveMaximum();
  }

  public WillTaskImpl getWillTaskImpl() {
    return willTaskImpl;
  }

  public WillTaskImpl setWillTask(WillDetails willDetails) {
    willTaskImpl =  WillTaskManager.getInstance().replace(getName(), willDetails);
    return willTaskImpl;
  }

  //</editor-fold>

  //<editor-fold desc="Subscription API">
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    if(isClosed){
      throw new IOException("Session is closed");
    }
    String originalName = context.getDestinationName();
    String namespace = destinationManager.calculateNamespace(originalName);
    namespaceMapping.addMapped(originalName, namespace);
    context.setDestinationName(namespace);
    return subscriptionManager.addSubscription(context);
  }

  public boolean removeSubscription(String id) {
    return subscriptionManager.delSubscription(id);
  }

  public void hibernateSubscription(String subscriptionId) {
    subscriptionManager.hibernateSubscription(subscriptionId);
  }

  public void addClosureTask(ClosureTask closureTask) {
    closureTaskManager.add(closureTask);
  }

  public String absoluteToNormalised(Destination destination) {
    String fqn = destination.getFullyQualifiedNamespace();
    String lookup = namespaceMapping.getOriginal(fqn);
    if(lookup == null){
      return fqn;
    }
    else{
      return lookup;
    }
  }

  //</editor-fold>

  private final class NamespaceMap{
    private final Map<String, String> originalToMapped;
    private final Map<String, String> mappedToOriginal;

    public NamespaceMap(){
      originalToMapped = new LinkedHashMap<>();
      mappedToOriginal = new LinkedHashMap<>();
    }

    public void clear(){
      originalToMapped.clear();
      mappedToOriginal.clear();
    }

    public void addMapped(String original, String mapped){
      originalToMapped.put(original, mapped);
      mappedToOriginal.put(mapped, original);
    }

    public String getMapped(String original){
      return originalToMapped.get(original);
    }

    public String getOriginal(String mapped){
      String located = mappedToOriginal.get(mapped);
      if(located == null){
        located = destinationManager.calculateOriginalNamespace(mapped);
        addMapped(located, mapped);
      }
      return located;
    }

    public void removeByMapped(String fullyQualifiedNamespace) {
      String found = mappedToOriginal.remove(fullyQualifiedNamespace);
      if(found != null){
        originalToMapped.remove(found);
      }
    }
  }

  private WillTaskImpl createWill(SessionContext sessionContext) throws IOException {
    if (sessionContext.getWillTopic() != null) {
      String willTopicName = destinationManager.calculateNamespace(context.getWillTopic());
      MessageDaemon.getInstance().getDestinationManager().findOrCreate(willTopicName);
      WillDetails willDetails =
          new WillDetails(
              sessionContext.getWillMessage(),
              willTopicName,
              sessionContext.getWillDelay(),
              sessionContext.getId(),
              sessionContext.getProtocol().getName(),
              sessionContext.getProtocol().getVersion());
      logger.log(ServerLogMessages.SESSION_MANAGER_WILL_TASK, sessionContext.getId(), willDetails.toString());
      return WillTaskManager.getInstance().replace(sessionContext.getId(), willDetails);
    }
    return null;
  }

}
