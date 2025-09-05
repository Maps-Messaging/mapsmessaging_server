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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.Subject;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class FakeProtocol extends Protocol {

  private static final AtomicLong counter = new AtomicLong(0);
  private MessageListener listener;

  public FakeProtocol(MessageListener listener) {
    super(new FakeEndPoint(counter.incrementAndGet(), null), new ProtocolConfigDTO());
    this.listener = listener;
    keepAlive = 10;
  }

  @Override
  public String getName() {
    return "Test Protocol";
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    listener.sendMessage(messageEvent);
  }

  @Override
  public boolean processPacket(Packet packet) {
    return true;
  }

  @Override
  public Subject getSubject() {
    return null;
  }

  @Override
  public void sendKeepAlive() {
    if(listener instanceof ProtocolMessageListener){
      ((ProtocolMessageListener)listener).sendKeepAlive();
    }
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    return null;
  }

  @Override
  public String getSessionId() {
    return "Test Session";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

}