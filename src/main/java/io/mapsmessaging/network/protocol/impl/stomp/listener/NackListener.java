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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.AcknowledgementToken;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Nack;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

public class NackListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer)
      throws StompProtocolException {
    Nack nackFrame = (Nack) frame;
    Acknowledgement acknowledgement = resolve(nackFrame, engine);
    SubscribedEventManager subscription = engine.findSubscription(acknowledgement.subscriptionId());
    if (subscription == null) {
      throw new StompProtocolException(
          "No subscription found that matches " + acknowledgement.subscriptionId());
    }
    subscription.rollbackReceived(acknowledgement.messageId());
  }

  private Acknowledgement resolve(Nack frame, SessionState engine)
      throws StompProtocolException {
    if (engine.getProtocol().isStomp12()) {
      AcknowledgementToken.Value value =
          AcknowledgementToken.parse(frame.getAcknowledgementId());
      return new Acknowledgement(value.subscriptionId(), value.messageId());
    }

    String subscriptionId = frame.getSubscription();
    String messageId = frame.getMessageId();
    if (subscriptionId == null || messageId == null) {
      throw new StompProtocolException(
          "STOMP 1.0/1.1 NACK requires subscription and message-id headers");
    }
    try {
      return new Acknowledgement(subscriptionId, Long.parseLong(messageId.trim()));
    } catch (NumberFormatException e) {
      throw new StompProtocolException("Invalid STOMP acknowledgement message-id");
    }
  }

  private record Acknowledgement(String subscriptionId, long messageId) {
  }
}
