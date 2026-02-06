/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.n2k;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.Packet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.mapsmessaging.logging.ServerLogMessages.N2K_PROTOCOL_INBOUND_HANDLER_ERROR;

public class InboundProcessor implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(InboundProcessor.class);
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final N2kProtocol protocol;

  public InboundProcessor(N2kProtocol protocol){
    this.protocol = protocol;
  }
  public void close(){
    running.set(false);
  }

  @Override
  public void run() {
    Packet packet = new Packet(10, false);
    while(running.get()){
      try {
        protocol.processPacket(packet);
      } catch (IOException e) {
        logger.log(N2K_PROTOCOL_INBOUND_HANDLER_ERROR, e.getMessage(), e);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }

  }
}
