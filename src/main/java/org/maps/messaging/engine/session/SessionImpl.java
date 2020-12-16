/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.messaging.engine.session;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.engine.closure.ClosureTask;
import org.maps.messaging.engine.closure.ClosureTaskManager;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.destination.DestinationManager;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.destination.subscription.SubscriptionController;
import org.maps.messaging.engine.session.will.WillTaskImpl;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class SessionImpl {
  private final Logger logger;
  private final SecurityContext securityContext;
  private final SessionContext context;
  private final Future<?> scheduledFuture;
  private final SubscriptionController subscriptionManager;
  private final DestinationManager destinationManager;
  private final WillTaskImpl willTaskImpl;
  private final ClosureTaskManager closureTaskManager;
  private MessageCallback messageCallback;
  private boolean isClosed;
  private long expiry;

  //<editor-fold desc="Life cycle API">
  SessionImpl(SessionContext context,
      SecurityContext securityContext,
      DestinationManager destinationManager,
      SubscriptionController subscriptionManager,
      WillTaskImpl willTaskImpl) {
    logger = LoggerFactory.getLogger(SessionImpl.class);
    this.securityContext = securityContext;
    this.context = context;
    this.subscriptionManager = subscriptionManager;
    this.destinationManager = destinationManager;
    this.willTaskImpl = willTaskImpl;
    closureTaskManager = new ClosureTaskManager();
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
      logger.log(LogMessages.SESSION_MANAGER_KEEP_ALIVE_TASK);
    } else {
      scheduledFuture = null;
    }
  }

  SubscriptionController getSubscriptionController() {
    return subscriptionManager;
  }

  void close() {
    logger.log(LogMessages.SESSION_MANAGER_CLOSING_SESSION, context.getId());
    isClosed = true;
    securityContext.logout();
    if (scheduledFuture != null) {
      scheduledFuture.cancel(true);
    }
    closureTaskManager.close();
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
  }

  public SecurityContext getSecurityContext(){
    return securityContext;
  }

  //</editor-fold>

  //<editor-fold desc="Destination Control API">
  public DestinationImpl findDestination(String destinationName, @Nullable DestinationType destinationType) throws IOException {
    if(isClosed){
      throw new IOException("Session is closed");
    }
    DestinationImpl destinationImpl = destinationManager.find(destinationName);
    if (destinationImpl == null && destinationType != null) {
      destinationImpl = destinationManager.create(destinationName, destinationType);
    }
    return destinationImpl;
  }

  public MessageCallback getMessageCallback() {
    return messageCallback;
  }

  public void setMessageCallback(MessageCallback messageCallback) {
    this.messageCallback = messageCallback;
  }

  public DestinationImpl deleteDestination(DestinationImpl destinationImpl) {
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
  //</editor-fold>

  //<editor-fold desc="Subscription API">
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    if(isClosed){
      throw new IOException("Session is closed");
    }
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

  //</editor-fold>

}
