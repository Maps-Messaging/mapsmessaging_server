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

package io.mapsmessaging.network.io.impl;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.ServerPacket;
import lombok.Getter;
import lombok.Setter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static io.mapsmessaging.logging.ServerLogMessages.WRITE_TASK_SEND_FAILED;

public class WriteTask implements Selectable {

  protected final FrameHandler frameHandler;
  protected final Logger logger;
  protected final SelectorTask selectorTask;
  protected final Queue<ServerPacket> outboundFrame;
  protected final SelectorCallback selectorCallback;

  @Getter
  @Setter
  private int coalesceSize;

  public WriteTask(SelectorCallback selectorCallback, int bufferSize, SelectorTask selectorTask, Logger logger) {
    this.selectorTask = selectorTask;
    this.selectorCallback = selectorCallback;
    this.logger = logger;
    frameHandler = new FrameHandler(this, bufferSize);
    outboundFrame = new ConcurrentLinkedDeque<>();
    coalesceSize = 100;
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    ThreadContext.put("endpoint", selectorCallback.getEndPoint().getName());
    ThreadContext.put("protocol", selectorCallback.getName());
    ThreadContext.put("session", selectorCallback.getSessionId());
    ThreadContext.put("version", selectorCallback.getVersion());
    try {
      handleWrite();
    } catch (Exception e) {
      logger.log(WRITE_TASK_SEND_FAILED, e);
    }
    ThreadContext.clearMap();
  }

  public void handleWrite() {
    frameHandler.processSelection();
  }

  public void push(ServerPacket frame) {
    outboundFrame.offer(frame);
    frameHandler.registerWrite();
  }

  public int size() {
    return outboundFrame.size();
  }


}
