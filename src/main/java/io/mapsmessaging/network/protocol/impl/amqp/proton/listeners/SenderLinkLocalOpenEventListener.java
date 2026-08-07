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
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.api.features.CreditHandler;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.SessionManager;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import io.mapsmessaging.security.uuid.UuidGenerator;
import io.mapsmessaging.selector.TokenMgrException;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SenderLinkLocalOpenEventListener extends LinkLocalOpenEventListener {


  public SenderLinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if (link instanceof Sender sender) {
      if (sender.getContext() instanceof SubscribedEventManager || sender.getContext() instanceof SubscriptionContext) {
        return true;
      }
      try {
        return prepareSender(event, sender);
      } catch (LoginException | IOException e) {
        failSenderOpen(sender, e);
      }
    }
    return false;
  }

  boolean prepareSender(Event event, Sender sender) throws LoginException, IOException {
    Source source = getSource(sender);
    String destinationName = resolveDestinationName(event, sender, source);
    return destinationName != null && processEvent(event, sender, sender, source, destinationName);
  }

  void failSenderOpen(Sender sender, Exception exception) {
    setSenderOpenFailure(sender, exception);
    sender.close();
  }

  void setSenderOpenFailure(Sender sender, Exception exception) {
    Source source = getSource(sender);
    if (source != null && source.getDynamic()) {
      sender.setCondition(new ErrorCondition(DYNAMIC_CREATION_ERROR, "Failed to create the dynamic destination::" + exception.getMessage()));
    }
  }

  private Source getSource(Sender sender) {
    if (sender.getRemoteSource() instanceof Source remoteSource) {
      return remoteSource;
    }
    return sender.getSource() instanceof Source localSource ? localSource : null;
  }

  private String resolveDestinationName(Event event, Sender sender, Source source) throws LoginException, IOException {
    if (source == null || !source.getDynamic()) {
      return getDestinationName(source);
    }

    Session session = getOrCreateSession(event);
    DestinationType destinationType = getDestinationType(source).isQueue() ? DestinationType.TEMPORARY_QUEUE : DestinationType.TEMPORARY_TOPIC;
    UUID uuid = UuidGenerator.getInstance().generate();
    String destinationName = "/dynamic/temporary/" + (destinationType.isQueue() ? "queue/" : "topic/") + uuid;
    try {
      Destination destination = session.findDestination(destinationName, destinationType).get();
      if (destination == null) {
        throw new IOException("Destination manager returned no dynamic destination");
      }
      source.setAddress(destinationName);
      sender.setSource(source);
      return destinationName;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while creating dynamic destination", e);
    } catch (ExecutionException e) {
      throw new IOException("Unable to create dynamic destination", e.getCause());
    }
  }

  private boolean processEvent(Event event, Link link, Sender sender, Source source, String destinationName) throws LoginException, IOException {
    int initialCredit = sender.getCredit();
    Symbol distribution = source.getDistributionMode();
    boolean browser = (distribution != null && distribution.toString().equalsIgnoreCase("copy"));

    SubscriptionContextBuilder contextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.INDIVIDUAL);
    contextBuilder.setQos(QualityOfService.AT_LEAST_ONCE)
        .setNoLocalMessages(source.getFilter() != null && source.getFilter().containsKey(Symbol.valueOf("no-local")))
        .setAllowOverlap(false)
        .setReceiveMaximum(initialCredit)
        .setAlias(destinationName)
        .setBrowserFlag(browser)
        .setCreditHandler(CreditHandler.CLIENT); // AMQP Link credit requires the AMQP engine / link to manage the credit
    getSelector(source, contextBuilder);
    getShareName(sender, contextBuilder);

    DestinationType destinationType = getDestinationType(source);
    if (isShared(source) && destinationType.isTopic() && !getShareName(sender, contextBuilder)) {
      link.setCondition(new ErrorCondition(SHARE_NAME_ERROR, "Must supply a share name"));
      throw new IOException("Must Supply a share name");
    } else {
      Session session;
      try {
        session = getOrCreateSession(event);
      } catch (LoginException | IOException e) {
        link.setCondition(new ErrorCondition(SESSION_CREATION, "Failed to construct a session::" + e.getMessage()));
        throw e;
      }
      handleSubscription(link, sender, browser, contextBuilder, session, destinationName, destinationType);
    }
    return true;
  }

  private void handleSubscription(Link link, Sender sender, boolean browser, SubscriptionContextBuilder contextBuilder, Session session, String destinationName,
      DestinationType destinationType)
      throws IOException {
    SubscriptionContext context = contextBuilder.build();
    try {
      Destination destination = session.findDestination(destinationName, destinationType).get();
      if (destination != null) {
        SubscribedEventManager eventManager = session.resume(destination);
        if (eventManager == null || browser) {
          eventManager = session.addSubscription(context);
        }
        if (eventManager == null) {
          engine.getSubscriptions().put(context, sender);
          link.setContext(context);
        } else {
          engine.getSubscriptions().put(eventManager, sender);
          link.setContext(eventManager);
        }
      }
      protocol.getLogger().log(ServerLogMessages.AMQP_CREATED_SUBSCRIPTION, destinationName, context.getAlias());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while resolving destination " + destinationName, e);
    } catch (ExecutionException e) {
      throw new IOException("Unable to resolve destination " + destinationName, e.getCause());
    } catch (IOException e) {
      ErrorCondition errorCondition = new ErrorCondition(SUBSCRIPTION_ERROR, "Failed to establish subscription::" + e.getMessage());
      Throwable throwable = e.getCause();
      if (throwable instanceof TokenMgrException) {
        errorCondition = new ErrorCondition(SELECTOR_ERROR, "Selector exception raised::" + throwable.getMessage());
      }
      link.setCondition(errorCondition);
      throw e;
    }
  }

  private Session getOrCreateSession(Event event) throws LoginException, IOException {
    Session session = (Session) event.getSession().getContext();
    if (session == null) {
      SessionManager sessionManager = createOrReuseSession(event.getConnection());
      session = sessionManager.getSession();
      event.getSession().setContext(session);
    }
    return session;
  }
}
