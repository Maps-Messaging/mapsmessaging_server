package org.maps.messaging.engine.destination.subscription.tasks;

import org.maps.messaging.engine.destination.subscription.impl.shared.SharedSubscription;
import org.maps.messaging.engine.destination.subscription.transaction.AcknowledgementController;
import org.maps.messaging.engine.tasks.EngineTask;
import org.maps.messaging.engine.tasks.Response;
import org.maps.messaging.engine.tasks.VoidResponse;

public class SharedSubscriptionTask extends EngineTask {
  private final SharedSubscription sharedSubscription;
  private final AcknowledgementController acknowledgementController;
  private final long messageId;
  private final boolean ack;

  public SharedSubscriptionTask(SharedSubscription sharedSubscription, AcknowledgementController acknowledgementController, long messageId, final boolean ack){
    this.sharedSubscription = sharedSubscription;
    this.acknowledgementController = acknowledgementController;
    this.messageId = messageId;
    this.ack = ack;
  }

  @Override
  public Response taskCall() {
    if(ack){
      acknowledgementController.ack(messageId);
      sharedSubscription.ackReceived(messageId);
    }
    else{
      acknowledgementController.rollback(messageId);
    }
    return new VoidResponse();
  }
}
