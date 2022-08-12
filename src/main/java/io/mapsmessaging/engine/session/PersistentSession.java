package io.mapsmessaging.engine.session;

import static io.mapsmessaging.logging.ServerLogMessages.SESSION_SAVE_STATE;
import static io.mapsmessaging.logging.ServerLogMessages.SESSION_SAVE_STATE_ERROR;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import java.io.FileOutputStream;
import java.io.IOException;

public class PersistentSession extends SessionImpl{

  private final SessionDetails sessionDetails;
  private final String storeName;

  PersistentSession(SessionContext context, SecurityContext securityContext, DestinationFactory destinationManager,
      SubscriptionController subscriptionManager, PersistentSessionManager storeLookup) {
    super(context, securityContext, destinationManager, subscriptionManager);
    sessionDetails = storeLookup.getSessionDetails(context.getId());
    context.setUniqueId(sessionDetails.getUniqueId());
    storeName = storeLookup.getDataPath() + "/" + sessionDetails.getUniqueId() + ".bin";
    if(context.isResetState()){
      saveState();
    }
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
    try(FileOutputStream fileOutputStream = new FileOutputStream(storeName)) {
      sessionDetails.save(fileOutputStream);
      logger.log(SESSION_SAVE_STATE, sessionDetails.getSessionName(), storeName);
    }
    catch(IOException ioException){
      logger.log(SESSION_SAVE_STATE_ERROR, sessionDetails.getSessionName(), storeName, ioException);
    }
  }

}
