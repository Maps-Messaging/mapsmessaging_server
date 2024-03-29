/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.MessageEvent;
import io.mapsmessaging.api.MessageListener;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class FakeProtocolImpl extends ProtocolImpl  {

  private static final AtomicLong counter = new AtomicLong(0);
  private MessageListener listener;

  public FakeProtocolImpl(MessageListener listener) {
    super(new FakeEndPoint(counter.incrementAndGet(), null));
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
  public void sendKeepAlive() {
    if(listener instanceof ProtocolMessageListener){
      ((ProtocolMessageListener)listener).sendKeepAlive();
    }
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