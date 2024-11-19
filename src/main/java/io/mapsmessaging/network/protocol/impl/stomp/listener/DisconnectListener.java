/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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

import io.mapsmessaging.network.protocol.impl.stomp.frames.CompletionHandler;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Receipt;
import io.mapsmessaging.network.protocol.impl.stomp.frames.ServerFrame;
import io.mapsmessaging.network.protocol.impl.stomp.state.ClosedState;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;

public class DisconnectListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    engine.changeState(new ClosedState());
    if (frame.getReceipt() != null) {
      ServerFrame receipt = new Receipt();
      receipt.setReceipt(frame.getReceipt());
      receipt.setCallback(new DisconnectCompletion(engine));
      frame.setCallback(null);
      engine.send(receipt);
    }
  }

  static class DisconnectCompletion implements CompletionHandler {

    private final StateEngine engine;

    public DisconnectCompletion(StateEngine engine) {
      this.engine = engine;
    }

    public void run() {
      engine.shutdown();
    }
  }
}
