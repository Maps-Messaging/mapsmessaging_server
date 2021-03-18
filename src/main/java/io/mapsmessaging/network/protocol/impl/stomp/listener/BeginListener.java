/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.TransactionException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Begin;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Error;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;

public class BeginListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    Begin begin = (Begin) frame;
    try {
      String transaction = begin.getTransaction();
      if (transaction != null && transaction.length() > 0) {
        engine.getSession().startTransaction(transaction);
      } else {
        Error error = new Error();
        error.setContent("No transaction name supplied".getBytes());
        if (frame.getReceipt() != null) {
          error.setReceipt(frame.getReceipt());
        }
        engine.send(error);
        frame.setReceipt(null); // its been handled
      }
    } catch (TransactionException e) {
      Error error = new Error();
      error.setContent(e.getMessage().getBytes());
      if (frame.getReceipt() != null) {
        error.setReceipt(frame.getReceipt());
      }
      engine.send(error);
      frame.setReceipt(null); // its been handled
    }
  }
}
