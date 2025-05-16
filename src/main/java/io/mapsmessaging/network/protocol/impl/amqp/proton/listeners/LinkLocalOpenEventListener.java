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

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.DescribedType;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.engine.Event.Type;

public class LinkLocalOpenEventListener extends BaseEventListener {


  public LinkLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  protected String getDestinationName(Source source) {
    if (source != null) {
      return source.getAddress();
    }
    return null;
  }

  @Override
  public boolean handleEvent(Event event) {
    // Check to see if we update the credit
    Link link = event.getLink();
    if (link instanceof Receiver) {
      topUp((Receiver) link);
    }
    return true;
  }

  @Override
  public EventType getType() {
    return Type.LINK_LOCAL_OPEN;
  }


  protected boolean getShareName(Sender sender, SubscriptionContextBuilder contextBuilder) {
    String shareName = sender.getName();
    if (shareName != null) {
      int location = shareName.indexOf('|');
      if (location > 0) {
        shareName = shareName.substring(0, location);
        contextBuilder.setSharedName(shareName);
        return true;
      }
    }
    return false;
  }

  //
  // Locates the selector within the source objects mapping and returns the string, if there is one
  //
  protected void getSelector(Source source, SubscriptionContextBuilder contextBuilder) {
    if (source.getFilter() != null) {
      Object filter = source.getFilter().get(Symbol.getSymbol("jms-selector"));
      if (filter instanceof DescribedType) {
        DescribedType decoder = (DescribedType) filter;
        String selector = decoder.getDescribed().toString();
        if (selector != null) {
          contextBuilder.setSelector(selector);
        }
      }
    }
  }
}
