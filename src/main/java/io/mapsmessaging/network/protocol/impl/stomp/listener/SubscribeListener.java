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

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.SubscriptionContextBuilder;
import io.mapsmessaging.api.features.ClientAcknowledgement;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Subscribe;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;

import java.io.IOException;

public class SubscribeListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) {
    ClientAcknowledgement ackManger;
    Subscribe subscribe = (Subscribe) frame;
    switch (subscribe.getAck()) {
      case "client":
        ackManger = ClientAcknowledgement.BLOCK;
        break;

      case "client-individual":
        ackManger = ClientAcknowledgement.INDIVIDUAL;
        break;

      default:
        ackManger = ClientAcknowledgement.AUTO;
    }
    SubscriptionContextBuilder builder = new SubscriptionContextBuilder(subscribe.getDestination(), ackManger);
    builder.setSharedName(subscribe.getShareName());
    String selector = subscribe.getSelector();
    if (selector != null) {
      builder.setSelector(selector);
    }
    builder.setAlias(subscribe.getId());
    int inFlight = subscribe.getReceiveMaximum();
    if (inFlight <= 0) {
      inFlight = (engine.getProtocol().getMaxReceiveSize());
    }
    builder.setReceiveMaximum(inFlight);
    try {
      engine.createSubscription(builder.build());
    } catch (IOException ioe) {
      Error error = new Error();
      error.setContent("Unable to find topic".getBytes());
      if (frame.getReceipt() != null) {
        error.setReceipt(frame.getReceipt());
      }
      engine.send(error);
    }
  }
}
