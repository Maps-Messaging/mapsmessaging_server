/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.AmqpTransactionCoordinator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslatorFactory;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Outcome;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.transaction.Coordinator;
import org.apache.qpid.proton.amqp.transaction.Declare;
import org.apache.qpid.proton.amqp.transaction.Declared;
import org.apache.qpid.proton.amqp.transaction.Discharge;
import org.apache.qpid.proton.amqp.transaction.TransactionalState;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.EventType;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sender;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DeliveryEventListener extends BaseEventListener {

  public DeliveryEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public EventType getType() {
    return Event.Type.DELIVERY;
  }

  @Override
  public boolean handleEvent(Event event) {
    Delivery delivery = event.getDelivery();
    Link deliveryLink = delivery.getLink();
    if (deliveryLink instanceof Sender) {
      processSenderOutcome(delivery);
    } else if (deliveryLink instanceof Receiver receiver) {
      handleReceiveEvent(event, delivery, receiver);
      topUp(receiver);
    }
    return true;
  }

  private void processSenderOutcome(Delivery delivery) {
    if (!delivery.isUpdated()) {
      return;
    }
    Object context = delivery.getContext();
    if (!(context instanceof SubscribedEventManager manager)) {
      return;
    }

    long messageId = unpackLong(delivery.getTag());
    DeliveryState state = delivery.getRemoteState();
    if (state instanceof TransactionalState transactionalState) {
      Outcome outcome = transactionalState.getOutcome();
      if (outcome != null && !engine.getTransactionCoordinator().enlist(transactionalState.getTxnId(), delivery, manager, messageId, outcome)) {
        manager.rollbackReceived(messageId);
        delivery.settle();
      }
      return;
    }

    if (state instanceof Accepted || state instanceof Rejected) {
      manager.ackReceived(messageId);
      delivery.settle();
    } else if (state instanceof Released || state instanceof Modified) {
      manager.rollbackReceived(messageId);
      delivery.settle();
    }
  }

  private long unpackLong(byte[] data) {
    long value = 0;
    for (int index = 0; index < data.length; index++) {
      value |= (long) (data[index] & 0xff) << (8 * index);
    }
    return value;
  }

  private void handleReceiveEvent(Event event, Delivery delivery, Receiver receiver) {
    if (delivery.isPartial()) {
      return;
    }
    if (!delivery.isReadable()) {
      if (delivery.remotelySettled()) {
        delivery.settle();
      }
      return;
    }
    Target remoteTarget = receiver.getRemoteTarget();
    try {
      if (remoteTarget instanceof Coordinator) {
        processTransaction(delivery, receiver, event);
      } else {
        processIncomingMessage(event, delivery, receiver);
      }
    } catch (LoginException | IOException e) {
      reject(delivery, DeliveryError, e.getMessage());
    } finally {
      if (receiver.getReceiverSettleMode() == ReceiverSettleMode.FIRST || delivery.remotelySettled()) {
        delivery.settle();
      }
    }
  }

  private void processTransaction(Delivery delivery, Receiver receiver, Event event) throws LoginException, IOException {
    org.apache.qpid.proton.message.Message protonMessage = parseIncomingMessage(receiver);
    if (!(protonMessage.getBody() instanceof AmqpValue amqpValue)) {
      reject(delivery, TransactionError, "Transaction command must use an AMQP value body");
      return;
    }

    Object transactionEvent = amqpValue.getValue();
    if (transactionEvent instanceof Declare) {
      Session session = getOrCreateSession(event);
      Declared declared = new Declared();
      declared.setTxnId(engine.getTransactionCoordinator().declare(session));
      delivery.disposition(declared);
    } else if (transactionEvent instanceof Discharge discharge) {
      if (engine.getTransactionCoordinator().discharge(discharge.getTxnId(), Boolean.TRUE.equals(discharge.getFail()))) {
        delivery.disposition(Accepted.getInstance());
      } else {
        reject(delivery, SymbolNames.TRANSACTION_UNKNOWN_ID, "Unknown transaction identifier");
      }
    } else {
      reject(delivery, TransactionError, "Unsupported transaction command");
    }
  }

  private Session getOrCreateSession(Event event) throws LoginException, IOException {
    Session session = (Session) event.getSession().getContext();
    if (session == null) {
      io.mapsmessaging.network.protocol.impl.amqp.SessionManager sessionManager = createOrReuseSession(event.getConnection());
      session = sessionManager.getSession();
      event.getSession().setContext(session);
    }
    return session;
  }

  private void processIncomingMessage(Event event, Delivery delivery, Receiver receiver) throws IOException {
    org.apache.qpid.proton.message.Message protonMessage = parseIncomingMessage(receiver);
    String destinationName = getDestinationName(receiver, protonMessage);
    if (destinationName == null || destinationName.isBlank()) {
      reject(delivery, DeliveryError, "Message delivery failed, no destination supplied");
      return;
    }

    MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(protonMessage.getMessageAnnotations());
    MessageBuilder messageBuilder;
    try {
      messageBuilder = translator.decode(new MessageBuilder(), protonMessage);
    } catch (RuntimeException e) {
      throw new IOException("Malformed AMQP message body", e);
    }
    messageBuilder.storeOffline(true);
    messageBuilder.setTransformation(protocol.getProtocolMessageTransformation());
    Message message = MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build();

    Transaction transaction = null;
    Binary transactionId = null;
    DeliveryState remoteState = delivery.getRemoteState();
    if (remoteState instanceof TransactionalState transactionalState) {
      transactionId = transactionalState.getTxnId();
      transaction = engine.getTransactionCoordinator().find(transactionId);
      if (transaction == null) {
        reject(delivery, SymbolNames.TRANSACTION_UNKNOWN_ID, "Unknown transaction identifier");
        return;
      }
    }

    Session session = (Session) event.getSession().getContext();
    if (session == null) {
      reject(delivery, DeliveryError, "AMQP session is not established");
      return;
    }

    Destination destination = findDestination(session, destinationName, getDestinationType(receiver, protonMessage));
    if (destination == null) {
      reject(delivery, NoSuchDestinationError, "The destination " + destinationName + " is not found or is not valid");
      return;
    }

    if (transaction != null) {
      transaction.add(destination, message);
      TransactionalState state = new TransactionalState();
      state.setTxnId(new Binary(AmqpTransactionCoordinator.copy(transactionId)));
      state.setOutcome(Accepted.getInstance());
      delivery.disposition(state);
    } else {
      destination.storeMessage(message);
      delivery.disposition(Accepted.getInstance());
    }
  }

  private Destination findDestination(Session session, String destinationName, DestinationType type) throws IOException {
    try {
      return session.findDestination(destinationName, type).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while resolving destination " + destinationName, e);
    } catch (ExecutionException e) {
      throw new IOException("Unable to resolve destination " + destinationName, e.getCause());
    }
  }

  private void reject(Delivery delivery, org.apache.qpid.proton.amqp.Symbol condition, String description) {
    Rejected rejected = new Rejected();
    rejected.setError(new ErrorCondition(condition, description));
    delivery.disposition(rejected);
  }

  private org.apache.qpid.proton.message.Message parseIncomingMessage(Receiver receiver) {
    org.apache.qpid.proton.message.Message protonMessage = org.apache.qpid.proton.message.Message.Factory.create();
    protonMessage.decode(receiver.recv());
    return protonMessage;
  }

  private static final class SymbolNames {

    private static final org.apache.qpid.proton.amqp.Symbol TRANSACTION_UNKNOWN_ID = org.apache.qpid.proton.amqp.Symbol.valueOf("amqp:transaction:unknown-id");

    private SymbolNames() {
    }
  }
}
