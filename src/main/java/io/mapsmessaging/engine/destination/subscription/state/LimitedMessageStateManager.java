package io.mapsmessaging.engine.destination.subscription.state;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.utilities.collections.bitset.BitSetFactory;
import io.mapsmessaging.utilities.queue.EventReaperQueue;

public class LimitedMessageStateManager extends MessageStateManagerImpl {

  private final int limit;
  private final EventReaperQueue eventReaperQueue;

  public LimitedMessageStateManager(String name, BitSetFactory bitsetFactory, int limit, EventReaperQueue eventReaperQueue) {
    super(name, bitsetFactory);
    this.limit = limit;
    this.eventReaperQueue = eventReaperQueue;
  }

  @Override
  public synchronized void register(Message message) {
    register(message.getIdentifier(), message.getPriority().getValue());
  }

  @Override
  public synchronized void register(long messageId) {
    register(messageId, Priority.ONE_BELOW_HIGHEST.getValue());
  }

  private void register(long id, int priority){
    messagesAtRest.add(id, priority);
    logger.log(ServerLogMessages.MESSAGE_STATE_MANAGER_REGISTER, name, id);
    trimAtRest();
  }

  private void trimAtRest(){
    while(messagesAtRest.size() > limit){
      long last = messagesAtRest.last();
      eventReaperQueue.add(last);
    }
  }
}
