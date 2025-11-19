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

package io.mapsmessaging.network.protocol.impl.amqp.proton.listeners;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.TransactionManager;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslator;
import io.mapsmessaging.network.protocol.impl.amqp.proton.transformers.MessageTranslatorFactory;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.transaction.*;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.*;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;

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
      delivery.settle();
    } else {
      handleReceiveEvent(event, delivery, (Receiver) deliveryLink);
    }
    processFlowControl(event);
    return true;
  }

  private void processFlowControl(Event event) {
    // Check to see if the remote end is acknowledged delivery of the event, in which case ack the event ID
    Link eventLink = event.getLink();
    Delivery dlv = event.getDelivery();
    if (eventLink instanceof Sender) {
      dlv.settle();
      long id = unpackLong(dlv.getTag());
      Object context = dlv.getContext();
      if (context instanceof SubscribedEventManager manager) {
        manager.ackReceived(id);
      }
    } else if (eventLink instanceof Receiver receiver) {
      topUp(receiver);
    }
  }

  private long unpackLong(byte[] buff) {
    long value = 0;
    for (int x = 0; x < buff.length; x++) {
      long val = buff[x];
      value ^= (val & 0xff) << (8 * x);
    }
    return value;
  }

  private void handleReceiveEvent(Event event, Delivery delivery, Receiver receiver) {
    // Check the remote target and see if it is of type Coordinator, if so, then we have a transactional event
    Target remoteTarget = receiver.getRemoteTarget();
    if (remoteTarget instanceof Coordinator) {
      try {
        processTransaction(delivery, receiver, event);
      } catch (LoginException | IOException e) {
        Rejected rejected = new Rejected();
        rejected.setError(new ErrorCondition(DeliveryError, "User not authorised on server"));
        delivery.disposition(rejected);
      }
    } else {
      if (!delivery.isPartial() && receiver.getTarget() != null) {
        String destinationName = receiver.getTarget().getAddress();
        if (destinationName != null) {
          processIncomingMessage(event, delivery, receiver, destinationName);
        } else {
          Rejected rejected = new Rejected();
          rejected.setError(new ErrorCondition(DeliveryError, "Message delivery failed, no destination supplied"));
          delivery.disposition(rejected);
        }
        delivery.settle();
      }
    }
  }

  private void processTransaction(Delivery delivery, Receiver receiver, Event event) throws LoginException, IOException {
    org.apache.qpid.proton.message.Message protonMsg = parseIncomingMessage(receiver);
    AmqpValue amqpValue = (AmqpValue) protonMsg.getBody();
    Object transactionEvent = amqpValue.getValue();

    // We have a Declare request, meaning this is to Declare a new transaction, so lets create one
    // add it to the context and see how we go
    if (transactionEvent instanceof Declare) {
      Session session = (Session) event.getSession().getContext();
      if (session == null) {
        io.mapsmessaging.network.protocol.impl.amqp.SessionManager sessionManager = createOrReuseSession(event.getConnection());
        session = sessionManager.getSession();
        event.getSession().setContext(session);
      }
      handleDeclare(delivery, receiver, session);
    }

    // If its a discharge it means we are completing the transaction, we either commit the transaction OR we abort it and unwind
    // all resources currently allocated for the transaction
    else if (transactionEvent instanceof Discharge dischargeEvent) {
      handleDischarge(dischargeEvent, delivery, receiver);
    }
    delivery.settle();
  }

  // Handle the creation of a transaction
  private void handleDeclare(Delivery delivery, Receiver receiver, Session session) {
    try {
      // Using nanoTime as a transaction ID, the chances of a transaction duplication is rare, for example,
      // the transaction must STILL be active after 106 days (Rollover of 2^63) AND hit at the same nanosecond
      // As long as the transaction managers life time is less than 106 days we are OK, else we run a risk of
      // 1 in 2^63 chance of hitting a duplicate
      String transactionId = "tx:" + System.nanoTime();
      Transaction transaction = session.startTransaction(transactionId);
      receiver.setContext(transaction);
      Declared declared = new Declared();
      declared.setTxnId(new Binary(transactionId.getBytes()));
      delivery.disposition(declared);
    } catch (IOException e) {
      Rejected rejected = new Rejected();
      rejected.setError(new ErrorCondition(DeliveryError, "Message delivery failed with exception : " + e.getMessage()));
      delivery.disposition(rejected);
    }
  }

  // We need to handle the discharge, basically commit or abort on the transaction
  private void handleDischarge(Discharge discharge, Delivery delivery, Receiver receiver) {
    String errorMsg = "";
    Transaction transaction = (Transaction) receiver.getContext();
    byte[] txId = null;
    // Validate the state here
    if (transaction == null) {
      errorMsg = "Transaction context not set";
    }
    if (discharge.getTxnId() != null) {
      txId = discharge.getTxnId().getArray();
    } else {
      errorMsg += " No Transaction ID supplied";
    }

    // If we have everything, then lets  continue with either commit / abort
    boolean success = false;
    if (txId != null && transaction != null && transaction.getTransactionId().equals(new String(txId))) {
      try {
        dischargeTransaction(discharge, transaction);
        success = true;
      } catch (IOException e) {
        errorMsg += "Transaction discharge failed with exception : " + e.getMessage();
      }
    } else {
      errorMsg = "Transaction ID does not match";
    }

    if (success) {
      delivery.disposition(Accepted.getInstance());
    } else {
      Rejected rejected = new Rejected();
      rejected.setError(new ErrorCondition(TransactionError, errorMsg));
      delivery.disposition(rejected);
    }
  }

  private void dischargeTransaction(Discharge discharge, Transaction transaction) throws IOException {
    if (Boolean.TRUE.equals(discharge.getFail())) {
      transaction.abort();
    } else {
      transaction.commit();
    }
  }

  // Process the incoming message and either push to the transaction or the destination
  @SneakyThrows
  private void processIncomingMessage(Event evt, Delivery delivery, Receiver receiver, String destinationName) {

    // Let's parse the data into a Proton Message so we can then create the appropriate internal message
    org.apache.qpid.proton.message.Message protonMsg = parseIncomingMessage(receiver);

    MessageTranslator translator = MessageTranslatorFactory.getMessageTranslator(protonMsg.getMessageAnnotations());
    MessageBuilder mb = new MessageBuilder();
    MessageBuilder messageBuilder = translator.decode(mb, protonMsg);
    messageBuilder.storeOffline(true);
    messageBuilder.setTransformation(protocol.getProtocolMessageTransformation());

    Message message = MessageOverrides.createMessageBuilder(protocol.getProtocolConfig().getMessageDefaults(), messageBuilder).build();
    DeliveryState deliveryState = delivery.getRemoteState();
    Transaction transaction = null;
    if (deliveryState instanceof TransactionalState transactionalState) {
      Binary binary = transactionalState.getTxnId();
      if (binary != null) {
        String transactionId = new String(binary.getArray());
        transaction = TransactionManager.getInstance().find(transactionId);
      }
    }

    Session session = (Session) evt.getSession().getContext();
    try {
      DestinationType type = getDestinationType(receiver);
      Destination destination = session.findDestination(destinationName, type).get();
      if (destination != null) {
        if (transaction != null) {
          transaction.add(destination, message);
        } else {
          destination.storeMessage(message);
        }
        delivery.disposition(Accepted.getInstance());
      } else {
        Rejected rejected = new Rejected();
        rejected.setError(new ErrorCondition(NoSuchDestinationError, "The destination " + destinationName + " is not found or is not valid"));
        delivery.disposition(rejected);
      }
    } catch (IOException e) {
      Rejected rejected = new Rejected();
      rejected.setError(new ErrorCondition(DeliveryError, "Message delivery failed with exception : " + e.getMessage()));
      delivery.disposition(rejected);
    }
  }

  //
  // OK Some info here.. AMQP does not really cover transactions, transactional IDs and semantics as such.
  // For this to happen a delivery message is used and encoded with the information required, so now we need to
  // decode the incoming data and figure out what we should do with it as far as the proton protocol is concerned
  //
  private org.apache.qpid.proton.message.Message parseIncomingMessage(@NonNull @NotNull Receiver receiver) {
    org.apache.qpid.proton.message.Message protonMsg = org.apache.qpid.proton.message.Message.Factory.create();
    protonMsg.decode(receiver.recv());
    return protonMsg;
  }
}
