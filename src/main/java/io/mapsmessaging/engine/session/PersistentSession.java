package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.engine.destination.DestinationFactory;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.engine.destination.subscription.SubscriptionController;
import io.mapsmessaging.engine.session.persistence.SessionDetails;
import io.mapsmessaging.engine.session.persistence.WillData;
import io.mapsmessaging.engine.session.will.WillDetails;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.yaml.snakeyaml.Yaml;

public class PersistentSession extends SessionImpl{

  private final SessionDetails sessionDetails;
  private final PersistentSessionManager storeLookup;
  private final String storeName;

  PersistentSession(SessionContext context, SecurityContext securityContext, DestinationFactory destinationManager,
      SubscriptionController subscriptionManager, PersistentSessionManager storeLookup) {
    super(context, securityContext, destinationManager, subscriptionManager);
    this.storeLookup = storeLookup;
    sessionDetails = storeLookup.getSessionDetails(context.getId());
    context.setUniqueId(sessionDetails.getUniqueId());
    storeName = storeLookup.getDataPath() + "/" + sessionDetails.getUniqueId() + ".yaml";
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

  @Override
  public WillTaskImpl setWillTask(WillDetails willDetails) {
    try {
      sessionDetails.setWillDetails(new WillData(willDetails));
      saveState();
    } catch (IOException e) {
      // ignore
    }
    return super.setWillTask(willDetails);
  }

  private void saveState(){
    Yaml yaml = new Yaml();
    try(FileOutputStream fileOutputStream = new FileOutputStream(storeName)) {
      OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
      yaml.dump(sessionDetails, writer);
    }
    catch(IOException ioException){

    }
  }

}
