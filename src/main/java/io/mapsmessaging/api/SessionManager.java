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

package io.mapsmessaging.api;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.session.SessionContext;
import io.mapsmessaging.engine.session.SessionImpl;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.*;

import static io.mapsmessaging.logging.ServerLogMessages.SESSION_ERROR_DURING_CREATION;

/**
 * Session lifetime management class. This class handles the life cycle of a Session, as well as the ability to perform anonymous publishes, if configured and allowed.
 */
@SuppressWarnings("java:S6548") // yes it is a singleton
public class SessionManager {

  private static class Holder {
    static final SessionManager INSTANCE = new SessionManager();
  }

  public static SessionManager getInstance() {
    return Holder.INSTANCE;
  }

  private final ExecutorService publisherScheduler;

  private final Logger logger = LoggerFactory.getLogger(SessionManager.class);

  /**
   * Creates a new Session using the supplied context and the message listener to deliver events that match any future subscriptions
   *
   * @param sessionContext The Session Context object containing session configuration
   * @param listener A callback object to handle all messages that match subscriptions
   * @return A new Session with any restored subscriptions, depending on the context
   * @throws LoginException Thrown if, during, the authentication phase the challenge fails
   * @throws IOException Thrown if unable to either restore the subscriptions or store the new session context
   */
  @SneakyThrows
  public @NonNull @NotNull Session create(@NonNull @NotNull SessionContext sessionContext, @NonNull @NotNull MessageListener listener) throws LoginException, IOException {
    return createAsync(sessionContext, listener).get();
  }

  public @NonNull @NotNull CompletableFuture<Session> createAsync(@NonNull @NotNull SessionContext sessionContext, @NonNull @NotNull MessageListener listener) {
    CompletableFuture<Session> completableFuture = new CompletableFuture<>();
    Callable<SessionImpl> task = () -> {
      SessionImpl sessionImpl = null;
      try {
        sessionImpl = MessageDaemon.getInstance().getSubSystemManager().getSessionManager().create(sessionContext);
        completableFuture.complete(new Session(sessionImpl, listener));
      }
      catch(Throwable t) {
        logger.log(SESSION_ERROR_DURING_CREATION, t);
        completableFuture.completeExceptionally(t);
      }
      return sessionImpl;
    };
    MessageDaemon.getInstance().getSubSystemManager().getSessionManager().submit(sessionContext.getId(), task);
    return completableFuture;
  }

  /**
   * Closes the supplied session, clears all resources and, if, the session is persistent marks all subscriptions as hibernated With the optional flag indicating that the WillTask
   * ( if supplied ) to be executed or to be cleared
   *
   * @param session The session to close
   * @param clearWillTask Flag indicating if the will task should be executed or not
   * @throws IOException Thrown if the underlying file system raises exception during the close phases
   */
  public void close(@NonNull @NotNull Session session, boolean clearWillTask) throws IOException {
    try {
      closeAsync(session, clearWillTask).get();
    } catch (ExecutionException e) {
      throw new IOException(e);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }
  }

  public @NonNull @NotNull CompletableFuture<Session> closeAsync(@NonNull @NotNull Session session, boolean clearWillTask) {
    CompletableFuture<Session> completableFuture = new CompletableFuture<>();
    Callable<Void> task = () -> {
      try {
        MessageDaemon.getInstance().getSubSystemManager().getSessionManager().close(session.getSession(), clearWillTask);
        session.close();
        completableFuture.complete(session);
      } catch (Exception e) {
        completableFuture.completeExceptionally(e);
      }
      return null;
    };
    MessageDaemon.getInstance().getSubSystemManager().getSessionManager().submit(session.getName(), task);
    return completableFuture;
  }


  /**
   * The function requires a simple path to inject a message onto a destination without the requirement of an existing session. This is useful for preconfigured bound
   * destinations.
   *
   * @param destination The destination that this message is bound for
   * @param message The message to send
   * @return If the publishing was successful then true, else false if it failed
   */
  public CompletableFuture<Integer> publish(@NonNull @NotNull String destination, @NonNull @NotNull Message message) {
    return CompletableFuture.supplyAsync(() -> {
          try {
            return MessageDaemon.getInstance().getDestinationManager().find(destination).get();
          } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
          }
        }, publisherScheduler)
        .thenCompose(destinationImpl -> {
          if (destinationImpl != null) {
            try {
              return CompletableFuture.completedFuture(destinationImpl.storeMessage(message));
            } catch (Throwable e) {
              return CompletableFuture.failedFuture(e);
            }
          } else {
            return CompletableFuture.completedFuture(-1);
          }
        });
  }


  private SessionManager() {
    publisherScheduler = Executors.newCachedThreadPool();
  }
}
