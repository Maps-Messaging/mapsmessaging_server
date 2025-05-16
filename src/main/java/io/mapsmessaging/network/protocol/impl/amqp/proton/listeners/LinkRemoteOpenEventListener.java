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

import io.mapsmessaging.network.protocol.impl.amqp.AMQPProtocol;
import io.mapsmessaging.network.protocol.impl.amqp.proton.ProtonEngine;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.engine.*;
import org.apache.qpid.proton.engine.Event.Type;

public class LinkRemoteOpenEventListener extends BaseEventListener {

  public LinkRemoteOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Link link = event.getLink();
    if (link != null) {
      if (link instanceof Sender) {
        handleSenderOpen((Sender) link);
      } else if (link instanceof Receiver) {
        handleReceiverOpen((Receiver) link);
      }

      // Adjust the credit for the link if its a receiver
      if (link instanceof Receiver) {
        topUp((Receiver) link);
      }
      return true;
    }
    return false;
  }


  private void handleReceiverOpen(Receiver receiver) {
    receiver.open();
  }

  private void handleSenderOpen(Sender sender) {
    // Setup the local source/target
    if (sender.getSource() == null) {
      // We don't have the source, but we can reattach to the name given
      String name = sender.getName();
      Source source = new Source();
      source.setAddress(name);
      source.setDurable(TerminusDurability.UNSETTLED_STATE);
      sender.setSource(source);
    } else {
      sender.setSource(sender.getRemoteSource());
    }
    sender.setTarget(sender.getRemoteTarget());
    sender.open();
    if (sender.getLocalState() != EndpointState.UNINITIALIZED) {
      sender.free();
      sender.advance();
    }
  }

  @Override
  public EventType getType() {
    return Type.LINK_REMOTE_OPEN;
  }
}
