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

package org.maps.network.protocol.impl.amqp.proton.listeners;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Sender;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.SubscriptionContextBuilder;
import org.maps.messaging.api.features.ClientAcknowledgement;
import org.maps.messaging.api.features.CreditHandler;
import org.maps.messaging.api.features.DestinationType;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.engine.destination.subscription.SubscriptionContext;
import org.maps.messaging.engine.selector.TokenMgrError;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.SessionManager;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class SenderLinkLocalOpenEventListener extends LinkLocalOpenEventListener {


  public SenderLinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if(link instanceof Sender) {
      Sender sender = (Sender) link;
      String destinationName = null;
      Source source = (Source) sender.getRemoteSource();
      if(source != null) {
        destinationName = source.getAddress();
      }
      int initialCredit = sender.getCredit();
      if(destinationName == null){
        return false;
      }
      Symbol distribution = source.getDistributionMode();
      boolean browser = false;
      if(distribution != null && distribution.toString().equalsIgnoreCase("copy")){
        browser = true;
      }
      org.maps.messaging.api.Session session = (org.maps.messaging.api.Session) event.getSession().getContext();
      if(session == null){
        try {
          SessionManager sessionManager = createOrReuseSession(event.getConnection());
          session = sessionManager.getSession();
          event.getSession().setContext(session);
        } catch (LoginException | IOException e) {
          link.setCondition(new ErrorCondition(SESSION_CREATION, "Failed to construct a session::"+e.getMessage()));
          link.close();
          return false;
        }
      }

      SubscriptionContextBuilder contextBuilder = new SubscriptionContextBuilder(destinationName, ClientAcknowledgement.BLOCK);
      contextBuilder.setQos(QualityOfService.AT_LEAST_ONCE)
          .setNoLocalMessages(false) // This should be able to be set to true
          .setAllowOverlap(false)
          .setReceiveMaximum(initialCredit)
          .setAlias(destinationName)
          .setBrowserFlag(browser)
          .setCreditHandler(CreditHandler.CLIENT); // AMQP Link credit requires the AMQP engine / link to manage the credit
      getSelector(source, contextBuilder);
      getShareName(sender, contextBuilder);

      DestinationType destinationType = getDestinationType(source);
      if (isShared(source) && destinationType.isTopic() && !getShareName(sender, contextBuilder)){
        link.setCondition(new ErrorCondition(SHARE_NAME_ERROR, "Must supply a share name"));
        link.close();
      }
      else {
        SubscriptionContext context = contextBuilder.build();
        try {
          Destination destination = session.findDestination(destinationName, destinationType);
          if (destination != null) {
            SubscribedEventManager eventManager = session.resume(destination);
            if (eventManager == null || browser) {
              eventManager = session.addSubscription(context);
            }
            engine.addSubscription(context.getAlias(), sender);
            link.setContext(eventManager);
          }
          protocol.getLogger().log(LogMessages.AMQP_CREATED_SUBSCRIPTION, destinationName, context.getAlias());
        } catch (IOException e) {
          ErrorCondition errorCondition = new ErrorCondition(SUBSCRIPTION_ERROR, "Failed to establish subscription::" + e.getMessage());
          Throwable throwable = e.getCause();
          if(throwable instanceof TokenMgrError){
            errorCondition =new ErrorCondition(SELECTOR_ERROR, "Selector exception raised::" + throwable.getMessage());
          }
          link.setCondition(errorCondition);
          link.close();
        }
      }
      return true;
    }
    return false;
  }
}
