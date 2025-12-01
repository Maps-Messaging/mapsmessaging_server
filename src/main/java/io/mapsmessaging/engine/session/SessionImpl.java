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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.auth.CreationAuthorisationCheck;
import io.mapsmessaging.api.auth.DestinationAuthorisationCheck;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.auth.ServerPermissions;
import io.mapsmessaging.dto.rest.session.SessionInformationDTO;
import io.mapsmessaging.engine.closure.ClosureTask;
import io.mapsmessaging.engine.closure.ClosureTaskManager;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import io.mapsmessaging.engine.session.will.WillTaskManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * The SessionImpl class is responsible for managing the session state, handling destination control,
 * and managing subscriptions in a messaging system.
 *
 * The class provides methods for managing the lifecycle of a session, such as starting, closing, and resuming the session.
 * It also provides methods for controlling destinations, such as checking if a destination exists, finding a destination,
 * and deleting a destination.
 *
 * The SessionImpl class also includes methods for managing subscriptions, such as adding and removing subscriptions,
 * as well as hibernating a subscription. It also provides methods for adding closure tasks and converting between
 * absolute and normalized destination names.
 *
 * The class uses various dependencies, such as the DestinationFactory, SubscriptionController, and ClosureTaskManager,
 * to perform its operations. It also utilizes a NamespaceMap to map original and mapped destination names.
 *
 * The class implements the Runnable interface and includes a KeepAliveTask inner class that sends keep-alive messages
 * to the client connection at regular intervals.
 *
 * The SessionImpl class is part of a larger messaging system and is designed to be used in conjunction with other classes
 * and components to provide a complete messaging solution.
 */
public class SessionImpl {

  protected final Logger logger;
  private final Future<?> scheduledFuture;
  private final SubscriptionController subscriptionManager;
  private final DestinationFactory destinationManager;
  private final NamespaceMap namespaceMapping;
  private final ClosureTaskManager closureTaskManager;
  /**
   * -- GETTER --
   *  Returns a boolean value indicating whether the session is closed or not.
   *
   * @return true if the session is closed, false otherwise
   */
  @Getter
  private boolean isClosed;

  @Getter
  private final SessionContext context;
  @Getter
  private final SecurityContext securityContext;
  @Getter
  private WillTaskImpl willTaskImpl;
  /**
   * -- SETTER --
   *  Sets the message callback for this session.
   *
   * @param messageCallback the message callback to be set
   */
  @Setter
  @Getter
  private MessageCallback messageCallback;
  @Getter
  private long expiry;

  //<editor-fold desc="Life cycle API">
  /**
   * Constructor for creating a SessionImpl object.
   *
   * @param context              The SessionContext object.
   * @param securityContext      The SecurityContext object.
   * @param destinationManager   The DestinationFactory object.
   * @param subscriptionManager  The SubscriptionController object.
   */
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
    namespaceMapping = new NamespaceMap(destinationManager);
    isClosed = false;
    if (context.getExpiry() == -1) {
      expiry = 24L * 60L * 60L; // One Day
    } else {
      expiry = context.getExpiry();
    }
    //
    // Schedule a keep alive
    //
    if (context.getClientConnection().getTimeOut() != 0) {
      long ka = context.getClientConnection().getTimeOut() + 5000L; // allow 5 seconds more
      scheduledFuture = SimpleTaskScheduler.getInstance().scheduleAtFixedRate(new KeepAliveTask(context.getClientConnection()), ka, ka, TimeUnit.MILLISECONDS);
      logger.log(ServerLogMessages.SESSION_MANAGER_KEEP_ALIVE_TASK);
    } else {
      scheduledFuture = null;
    }
  }

  SubscriptionController getSubscriptionController() {
    return subscriptionManager;
  }

  /**
   * Closes the session.
   * This method performs the following actions:
   * 1. Logs a closing session message using the logger.
   * 2. Sets the 'isClosed' flag to true.
   * 3. Calls the 'logout' method of the security context to perform any necessary cleanup.
   * 4. Cancels the scheduled future, if it is not null.
   * 5. Closes the closure task manager.
   * 6. Clears the namespace mapping.
   */
  void close() {
    logger.log(ServerLogMessages.SESSION_MANAGER_CLOSING_SESSION, context.getId());
    isClosed = true;
    securityContext.logout();
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
    closureTaskManager.close();
    namespaceMapping.clear();
    if(!context.isPersistentSession()){
      subscriptionManager.close();
    }
  }

  /**
   * Resumes the state of the session by waking up all subscriptions.
   */
  public void resumeState() {
    subscriptionManager.wakeAll(this);
  }

  /**
   * Resumes the session by waking up the subscription manager for the specified destination.
   *
   * @param destination The destination to resume the session for.
   * @return The subscribed event manager for the resumed session.
   */
  public SubscribedEventManager resume(DestinationImpl destination) {
    return subscriptionManager.wake(this, destination);
  }

  /**
   * Starts the session by waking up the subscription manager.
   */
  public void start() {
    subscriptionManager.wake(this);
  }

  /**
   * Logs in the session by calling the login method of the security context.
   * Sets the session tenant configuration using the TenantManagement.build method.
   * Creates a will task for the session.
   *
   * @throws IOException if an I/O error occurs during the login process.
   */
  public void login() throws IOException {
    securityContext.login();
    ((SessionDestinationManager) destinationManager).setSessionTenantConfig(TenantManagement.build(context.getClientConnection(), securityContext));
    // Only do this once the connection has be authenticated
    try {
      this.willTaskImpl = createWill(context).get(10, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
  }

  //</editor-fold>

  //<editor-fold desc="Destination Control API">
  /**
   * Checks if a destination with the given name exists.
   *
   * @param destinationName the name of the destination to check
   * @return a CompletableFuture that completes with the DestinationImpl object if the destination exists,
   *         or completes exceptionally if there was an error or the destination does not exist
   */
  public CompletableFuture<DestinationImpl> destinationExists(@NonNull @NotNull String destinationName) {
    String mapped = namespaceMapping.getMapped(destinationName);
    if (mapped == null) {
      mapped = destinationManager.calculateNamespace(destinationName);
      namespaceMapping.addMapped(destinationName, mapped);
    }
    String finalMapped = mapped;

    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    future.completeAsync(() -> {
      try {
        return destinationManager.find(finalMapped).get();
      } catch (InterruptedException e){
        Thread.currentThread().interrupt();
        future.completeExceptionally(e);
      }
      catch (ExecutionException e) {
        future.completeExceptionally(e);
      }
      return null;
    });
    return future;
  }

  @SneakyThrows
  /**
   * This method finds a destination based on the given destination name and destination type.
   * If the session is closed, it throws an IOException.
   * It first checks if the destination name is already mapped in the namespace mapping.
   * If not, it calculates the namespace for the destination name using the destination manager.
   * Then, it checks if the destination already exists by calling the find method of the destination manager.
   * If the destination exists, it completes the future with the existing destination.
   * If the destination does not exist, it creates a callable task that creates the destination using the destination manager.
   * The callable task is then executed asynchronously and the future is completed with the created destination.
   * If any exception occurs during the process, the future is completed exceptionally.
   * Finally, the future is returned.
   *
   * @param destinationName The name of the destination to find or create
   * @param destinationType The type of the destination to find or create
   * @return A CompletableFuture that completes with the found or created destination
   * @throws IOException If the session is closed
   */
  public CompletableFuture<DestinationImpl> findDestination(@NonNull @NotNull String destinationName, @NonNull @NotNull DestinationType destinationType, DestinationAuthorisationCheck authCheck) throws IOException {
    if (isClosed) {
      throw new IOException("Session is closed");
    }
    String mapped = namespaceMapping.getMapped(destinationName);
    if (mapped == null) {
      mapped = destinationManager.calculateNamespace(destinationName);
      namespaceMapping.addMapped(destinationName, mapped);
    }
    DestinationImpl existing = destinationManager.find(mapped).get();

    String finalMapped = mapped;
    CompletableFuture<DestinationImpl> future = new CompletableFuture<>();
    if (existing != null) {
      future.complete(existing);
    } else {
      Callable<DestinationImpl> callable = () -> {
        DestinationImpl created = null;
        try {
          CompletableFuture<DestinationImpl> creationFuture = destinationManager.create(finalMapped, destinationType, authCheck);
          created = creationFuture.get();
          future.complete(created);
        } catch (IOException | ExecutionException e) {
          future.completeExceptionally(e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        catch(Throwable th){
          future.completeExceptionally(th);
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

  /**
   * Deletes the specified destination from the session.
   *
   * @param destinationImpl the destination to be deleted
   * @return a CompletableFuture that completes when the destination is successfully deleted
   */
  public CompletableFuture<DestinationImpl> deleteDestination(DestinationImpl destinationImpl) {
    ProtectedResource protectedResource  = new  ProtectedResource(destinationImpl.getResourceType().getName(), destinationImpl.getFullyQualifiedNamespace(), null);
    if(!AuthManager.getInstance().canAccess(securityContext.getIdentity(), ServerPermissions.DELETE ,protectedResource)){
      CompletableFuture<DestinationImpl>  future = new CompletableFuture<>();
      future.completeExceptionally(new IOException("Access denied"));
      return future;
    }

    namespaceMapping.removeByMapped(destinationImpl.getFullyQualifiedNamespace());
    return destinationManager.delete(destinationImpl);
  }
  //</editor-fold>

  //<editor-fold desc="Session state API">
  /**
   * Returns the name of the session.
   *
   * @return the name of the session
   */
  public String getName() {
    return context.getId();
  }

  /**
   * Returns the client connection associated with this session.
   *
   * @return the client connection
   */
  public ClientConnection getClientConnection() {
    return context.getClientConnection();
  }

  /**
   * Returns a boolean value indicating whether the session has been restored.
   *
   * @return true if the session has been restored, false otherwise
   */
  public boolean isRestored() {
    return context.isRestored();
  }

  /**
   * Sets the expiry time for the session.
   *
   * @param expiry the expiry time in milliseconds
   */
  public void setExpiryTime(long expiry) {
    this.expiry = expiry;
  }

  /**
   * Returns the maximum number of messages that can be received by this session.
   *
   * @return The maximum number of messages that can be received.
   */
  public int getReceiveMaximum() {
    return context.getReceiveMaximum();
  }

  /**
   * Sets the will task for the session.
   *
   * @param willDetails the details of the will task
   * @return the WillTaskImpl object representing the set will task
   */
  public WillTaskImpl setWillTask(WillDetails willDetails) {
    return WillTaskManager.getInstance().replace(getName(), willDetails);
  }

  //</editor-fold>

  //<editor-fold desc="Subscription API">
  /**
   * Adds a subscription to the session.
   *
   * @param context the subscription context
   * @return the subscribed event manager
   * @throws IOException if the session is closed
   */
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    if (isClosed) {
      throw new IOException("Session is closed");
    }

    String originalName = context.getDestinationName();
    String namespace = destinationManager.calculateNamespace(originalName);
    namespaceMapping.addMapped(originalName, namespace);
    context.setDestinationName(namespace);
    context.setAllocatedId( getContext().getInternalSessionId());
    subscriptionManager.wake(this);
    return subscriptionManager.addSubscription(context);
  }

  /**
   * Removes a subscription with the given ID.
   *
   * @param id the ID of the subscription to be removed
   * @return true if the subscription was successfully removed, false otherwise
   */
  public boolean removeSubscription(String id) {
    return subscriptionManager.delSubscription(id);
  }

  /**
   * Hibernate a subscription with the given subscription ID.
   *
   * @param subscriptionId the ID of the subscription to hibernate
   */
  public void hibernateSubscription(String subscriptionId) {
    subscriptionManager.hibernateSubscription(subscriptionId);
  }

  public void addClosureTask(ClosureTask closureTask) {
    closureTaskManager.add(closureTask);
  }


  /**
   * Returns the normalized version of the fully qualified namespace of the given destination.
   * If the namespace has been previously mapped, the original namespace is returned.
   * Otherwise, the fully qualified namespace is returned as is.
   *
   * @param destination The destination for which to retrieve the normalized namespace.
   * @return The normalized version of the fully qualified namespace.
   */
  public String absoluteToNormalised(Destination destination) {
    String fqn = destination.getFullyQualifiedNamespace();
    String lookup = namespaceMapping.getOriginal(fqn);
    if (lookup == null) {
      return fqn;
    } else {
      return lookup;
    }
  }

  //</editor-fold>

  /**
   * Creates a WillTaskImpl object based on the given SessionContext.
   * If the sessionContext's willTopic is not null, it calculates the willTopicName using the destinationManager's calculateNamespace method.
   * Then it finds or creates the destination with the calculated willTopicName using the MessageDaemon's destinationManager.
   * It creates a WillDetails object with the sessionContext's willMessage, willTopicName, willDelay, sessionContext's id, sessionContext's clientConnection's name, and sessionContext's clientConnection's version.
   * It logs the creation of the WillTaskImpl object using the logger.
   * Finally, it sets the WillTaskImpl object as the willTask of the SessionImpl object and returns it.
   * If the sessionContext's willTopic is null, it returns null.
   */
  private CompletableFuture<WillTaskImpl> createWill(SessionContext sessionContext) {
    if (sessionContext.getWillTopic() != null) {

      String willTopicName = destinationManager.calculateNamespace(context.getWillTopic());
      CreationAuthorisationCheck check = new CreationAuthorisationCheck(sessionContext.getSecurityContext().getIdentity());

      return MessageDaemon.getInstance()
          .getDestinationManager()
          .findOrCreate(willTopicName, check)
          .thenCompose(destinationImpl -> {
            WillDetails willDetails =
                new WillDetails(
                    sessionContext.getWillMessage(),
                    willTopicName,
                    sessionContext.getWillDelay(),
                    sessionContext.getId(),
                    sessionContext.getClientConnection().getName(),
                    sessionContext.getClientConnection().getVersion());

            logger.log(
                ServerLogMessages.SESSION_MANAGER_WILL_TASK,
                sessionContext.getId(),
                willDetails.toString());

            CompletableFuture<WillTaskImpl> willTaskFuture = new CompletableFuture<>();
            willTaskFuture.complete(this.setWillTask(willDetails));
            return willTaskFuture;
          });
    }
    CompletableFuture<WillTaskImpl> willTaskFuture = new CompletableFuture<>();
    willTaskFuture.complete(null);
    return willTaskFuture;
  }

  public SessionInformationDTO getSessionInformation() {
    SessionInformationDTO sessionInformationDTO = new SessionInformationDTO();
    sessionInformationDTO.setSessionInfo(context.getDetails());
    sessionInformationDTO.setSubscriptionInfo(subscriptionManager.getSubscriptionInformation());
    return sessionInformationDTO;
  }
}

