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

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.security.SecurityContext;
import lombok.Getter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static io.mapsmessaging.logging.ServerLogMessages.SESSION_SAVE_STATE;
import static io.mapsmessaging.logging.ServerLogMessages.SESSION_SAVE_STATE_ERROR;

public class PersistentSession extends SessionImpl{

  private final SessionDetails sessionDetails;
  @Getter
  private final String storeName;

  PersistentSession(SessionContext context, SecurityContext securityContext, DestinationFactory destinationManager,
      SubscriptionController subscriptionManager, PersistentSessionManager storeLookup) {
    super(context, securityContext, destinationManager, subscriptionManager);
    sessionDetails = storeLookup.getSessionDetails(context);
    context.setUniqueId(sessionDetails.getUniqueId());
    storeName = storeLookup.getDataPath() + "/" + sessionDetails.getUniqueId() + ".bin";
    if(context.isResetState() || sessionDetails.getSubscriptionContextList().isEmpty()){ // this will delete it and recreate
      sessionDetails.getSubscriptionContextList().clear(); // ensure it is clear
      saveState();
    }
  }
  @Override
  public void setExpiryTime(long expiry) {
    super.setExpiryTime(expiry);
    sessionDetails.setExpiryTime(expiry);
    saveState();
  }


  @Override
  public SubscribedEventManager addSubscription(SubscriptionContext context) throws IOException {
    SubscribedEventManager eventManager = super.addSubscription(context);
    sessionDetails.getSubscriptionContextList().add(context);
    saveState();
    return eventManager;
  }

  @Override
  public boolean removeSubscription(String id) {
    sessionDetails.getSubscriptionContextList().removeIf(subscriptionContext -> subscriptionContext.getFilter().equals(id)) ;
    saveState();
    return super.removeSubscription(id);
  }

  private void saveState(){
    if(sessionDetails.getSubscriptionContextList().isEmpty()){
      try{
        Files.deleteIfExists(Paths.get(storeName));
      } catch (IOException ioException) {
        logger.log(SESSION_SAVE_STATE_ERROR, sessionDetails.getSessionName(), storeName, ioException);
      }
    }
    else {
      try (FileOutputStream fileOutputStream = new FileOutputStream(storeName)) {
        sessionDetails.save(fileOutputStream);
        logger.log(SESSION_SAVE_STATE, sessionDetails.getSessionName(), storeName);
      } catch (IOException ioException) {
        logger.log(SESSION_SAVE_STATE_ERROR, sessionDetails.getSessionName(), storeName, ioException);
      }
    }
  }

}
