package io.mapsmessaging.engine.destination.tasks;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.impl.DestinationSubscription;
import io.mapsmessaging.engine.tasks.EngineTask;
import io.mapsmessaging.engine.tasks.Response;
import io.mapsmessaging.engine.tasks.ValueResponse;


public class NextMessageTask extends EngineTask {

  private final DestinationSubscription subscription;

  public NextMessageTask(DestinationSubscription subscription) {
    super();
    this.subscription = subscription;
  }

  @Override
  public Response taskCall() throws Exception {
    if(subscription.getDestinationImpl().isClosed()){
      return new ValueResponse<Message>(null);
    }
    return new ValueResponse<>(subscription.rawGetNext());
  }
}