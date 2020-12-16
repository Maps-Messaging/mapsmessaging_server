/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.protocol.impl.nmea;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import org.jetbrains.annotations.NotNull;
import org.maps.messaging.api.Destination;
import org.maps.messaging.api.MessageBuilder;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.messaging.api.SubscribedEventManager;
import org.maps.messaging.api.features.QualityOfService;
import org.maps.messaging.api.message.Message;
import org.maps.network.io.EndPoint;
import org.maps.network.io.Packet;
import org.maps.network.io.StreamEndPoint;
import org.maps.network.io.impl.SelectorTask;
import org.maps.network.protocol.EndOfBufferException;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.transformation.TransformationManager;

public class NMEAProtocol extends ProtocolImpl {

  private final Session session;
  private final Destination raw;
  private final SelectorTask selectorTask;
  private final Map<String, Destination> sentenceMap;


  public NMEAProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint);
    if (endPoint instanceof StreamEndPoint) {
      ((StreamEndPoint) endPoint).setStreamHandler(new NMEAStreamHandler());
    }
    if (packet != null) {
      packet.clear();
    }
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("NMEA" + endPoint.getId(), this);
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setKeepAlive(0);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    raw = session.findDestination("$NMEA/raw");
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties());
    sentenceMap = new LinkedHashMap<>();
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    setTransformation(TransformationManager.getInstance().getTransformation(getName(), null));
  }

  @Override
  public void sendMessage(@NotNull Destination destination, @NotNull SubscribedEventManager subscription, @NotNull Message message, @NotNull Runnable completionTask) {
    // This protocol is read only
  }

  @Override
  public void sendKeepAlive() {
    // It has no keep alive mechanism
  }

  @Override
  public boolean processPacket(Packet packet) throws IOException {
    while (packet.hasRemaining()) {
      int pos = packet.position();
      try {
        byte[] buffer = new byte[packet.available()];
        packet.get(buffer);
        String sentence = new String(buffer);
        String sentenceId = sentence.substring(0, sentence.indexOf(','));
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(sentence.getBytes());
        messageBuilder.storeOffline(false);
        messageBuilder.setRetain(false);
        messageBuilder.setMessageExpiryInterval(8,  TimeUnit.SECONDS); // Expire the event in 8 seconds
        messageBuilder.setQoS(QualityOfService.AT_MOST_ONCE);
        raw.storeMessage(messageBuilder.build());
        Destination destination = sentenceMap.get(sentenceId);
        if (destination == null) {
          destination = session.findDestination("$NMEA/sentence/" + sentenceId);
          sentenceMap.put(sentenceId, destination);
        }
        messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(sentence.getBytes())
            .setRetain(false)
            .storeOffline(false)
            .setMessageExpiryInterval(8, TimeUnit.SECONDS) // Expire the event in 8 seconds
            .setQoS(QualityOfService.AT_MOST_ONCE)
            .setTransformation(getTransformation());

        destination.storeMessage(messageBuilder.build());
        receivedMessage();
      } catch (EndOfBufferException e) {
        packet.position(pos);
      }
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }

  @Override
  public String getName() {
    return "NMEA";
  }

  @Override
  public String getSessionId() {
    return "GPS_" + endPoint.getName();
  }

  @Override
  public String getVersion() {
    return "0.1";
  }
}
