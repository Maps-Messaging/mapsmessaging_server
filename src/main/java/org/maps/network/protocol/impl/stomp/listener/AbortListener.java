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
import org.maps.logging.LogMessages;
import org.maps.messaging.api.Transaction;
import org.maps.network.protocol.impl.stomp.StompProtocolException;
import org.maps.network.protocol.impl.stomp.frames.Abort;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

public class AbortListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) throws StompProtocolException {
    Abort abort = (Abort) frame;
    Transaction transaction = find(abort, engine);
    try {
      transaction.abort();
      engine.getSession().closeTransaction(transaction);
    } catch (IOException e) {
      engine.getProtocol().getLogger().log(LogMessages.TRANSACTION_EXCEPTION, transaction,  e);
    }
  }
}
