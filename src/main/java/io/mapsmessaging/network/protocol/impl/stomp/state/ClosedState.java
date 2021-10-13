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

package io.mapsmessaging.network.protocol.impl.stomp.state;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.destination.subscription.SubscriptionContext;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import java.io.IOException;

public class ClosedState implements State {

  public void handleFrame(StateEngine engine, Frame frame, boolean endOfBuffer) throws IOException {
    throw new IOException("State is closed");
  }

  @Override
  public boolean sendMessage(StateEngine engine, String destinationName, SubscriptionContext info, Message message, Runnable completionTask) {
    return false;
  }
}
