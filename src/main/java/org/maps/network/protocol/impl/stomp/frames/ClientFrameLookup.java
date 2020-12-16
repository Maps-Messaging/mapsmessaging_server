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

package org.maps.network.protocol.impl.stomp.frames;

import org.maps.network.protocol.impl.stomp.listener.ClientFrameListener;

public class ClientFrameLookup {

  private byte[] command;
  private ClientFrame clientFrame;
  private ClientFrameListener frameListener;

  ClientFrameLookup(byte[] command, ClientFrame clientFrame, ClientFrameListener frameListener) {
    this.command = command;
    this.clientFrame = clientFrame;
    this.frameListener = frameListener;
  }

  public byte[] getCommand() {
    return command;
  }

  public ClientFrame getClientFrame() {
    return clientFrame;
  }

  public ClientFrameListener getFrameListener() {
    return frameListener;
  }
}
