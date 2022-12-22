/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

import io.mapsmessaging.network.protocol.impl.stomp.frames.Frame;
import io.mapsmessaging.network.protocol.impl.stomp.frames.Unsubscribe;
import io.mapsmessaging.network.protocol.impl.stomp.state.StateEngine;

public class UnsubscribeListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) {
    Unsubscribe unsubscribe = (Unsubscribe) frame;
    engine.removeSubscription(unsubscribe.getId());
  }
}