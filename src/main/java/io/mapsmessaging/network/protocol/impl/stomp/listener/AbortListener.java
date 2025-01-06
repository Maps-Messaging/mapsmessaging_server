/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.stomp.listener;

import io.mapsmessaging.api.Transaction;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.protocol.impl.stomp.StompProtocolException;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Abort;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.state.SessionState;
import java.io.IOException;

public class AbortListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, SessionState engine, boolean endOfBuffer) throws StompProtocolException {
    Abort abort = (Abort) frame;
    Transaction transaction = find(abort, engine);
    try {
      transaction.abort();
      engine.getSession().closeTransaction(transaction);
    } catch (IOException e) {
      engine.getProtocol().getLogger().log(ServerLogMessages.TRANSACTION_EXCEPTION, transaction, e);
    }
  }
}
