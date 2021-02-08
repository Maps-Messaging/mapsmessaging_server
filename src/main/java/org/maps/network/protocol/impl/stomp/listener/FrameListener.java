/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.listener;

import java.io.IOException;
import org.maps.messaging.api.Transaction;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.frames.ClientTransaction;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.frames.Receipt;
import org.maps.network.protocol.impl.stomp.frames.ServerFrame;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

public interface FrameListener {

  void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) throws IOException;

  default void postFrameHandling(Frame frame, StateEngine engine) {
    if (frame.getReceipt() != null) {
      ServerFrame receipt = new Receipt();
      receipt.setReceipt(frame.getReceipt());
      receipt.setCallback(frame.getCallback());
      frame.setCallback(null);
      engine.send(receipt);
    } else {
      frame.complete();
    }
  }

  default Transaction find(ClientTransaction transactionFrame, StateEngine engine) throws StompProtocolException {
    String transaction = transactionFrame.getTransaction();
    if (transaction == null || transaction.length() == 0) {
      throw new StompProtocolException("Illegal transaction ID received");
    }
    Transaction clientTransaction = engine.getSession().getTransaction(transaction);
    if (clientTransaction == null) {
      throw new StompProtocolException("No known client transaction found " + transaction);
    }
    return clientTransaction;
  }
}
