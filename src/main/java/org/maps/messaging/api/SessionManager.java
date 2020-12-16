/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.messaging.api;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.MessageDaemon;
import org.maps.messaging.api.message.Message;
import org.maps.messaging.engine.destination.DestinationImpl;
import org.maps.messaging.engine.session.SessionContext;
import org.maps.messaging.engine.session.SessionImpl;

/**
 * Session life time management class. This class handles the life cycle of a Session, as well as the ability to
 * perform anonymous publishes, if configured and allowed.
 */
public class SessionManager {

  private static final SessionManager instance = new SessionManager();

  /**
   * This is a singleton class and, as such, can only be accessed via this function
   *
   * @return Returns the singleton instance of this class
   */
  public static SessionManager getInstance() {
    return instance;
  }

  /**
   * Creates a new Session using the supplied context and the message listener to deliver events
   * that match any future subscriptions
   *
   * @param sessionContext The Session Context object containing session configuration
   * @param listener A callback object to handle all messages that match subscriptions
   * @return A new Session with any restored subscriptions, depending on the context
   * @throws LoginException Thrown if, during, the authentication phase the challenge fails
   * @throws IOException Thrown if unable to either restore the subscriptions or store the new session context
   */
  public @NotNull Session create(@NotNull SessionContext sessionContext, @NotNull MessageListener listener) throws LoginException, IOException {
    SessionImpl session = MessageDaemon.getInstance().getSessionManager().create(sessionContext);
    return new Session(session, listener);
  }

  /**
   * Closes the supplied session, clears all resources and, if, the session is persistent marks all subscriptions as hibernated
   * @param session The session to close
   * @throws IOException Thrown if the underlying file system raises exception during the close phases
   */
  public void close(@NotNull Session session) throws IOException {
    MessageDaemon.getInstance().getSessionManager().close(session.getSession());
    session.close();
  }

  /**
   * Closes the supplied session, clears all resources and, if, the session is persistent marks all subscriptions as hibernated
   * With the optional flag indicating that the WillTask ( if supplied ) to be executed or to be cleared
   *
   * @param session The session to close
   * @param clearWillTask Flag indicating if the will task should be executed or not
   * @throws IOException Thrown if the underlying file system raises exception during the close phases
   */
  public void close(@NotNull Session session, boolean clearWillTask) throws IOException {
    MessageDaemon.getInstance().getSessionManager().close(session.getSession(), clearWillTask);
    session.close();
  }

  /**
   * The function requires a simple path to inject a message onto a destination without the requirement of
   * an existing session. This is useful for preconfigured bound destinations.
   *
   * @param destination The destination that this message is bound for
   * @param message The message to send
   * @return If the publish was successful then true, else false if it failed
   *
   * @throws IOException Thrown if the message write failed, typically due to file system errors
   */
  public boolean publish(@NotNull String destination,@NotNull  Message message) throws IOException {
    DestinationImpl destinationImpl =
        MessageDaemon.getInstance().getDestinationManager().find(destination);
    if(destinationImpl != null) {
      destinationImpl.storeMessage(message);
      return true;
    }
    return false;
  }

  private SessionManager(){
  }
}
