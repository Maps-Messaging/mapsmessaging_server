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

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.api.*;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.protocol.ProtocolInformationDTO;
import io.mapsmessaging.dto.rest.protocol.impl.NmeaProtocolInformation;
import io.mapsmessaging.engine.destination.MessageOverrides;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.network.ProtocolClientConnection;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamEndPoint;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.SentenceFactory;
import io.mapsmessaging.network.protocol.impl.nmea.types.PositionType;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NMEAProtocol extends Protocol {

  private final Session session;
  private final SelectorTask selectorTask;
  private final Map<String, Destination> sentenceMap;
  private final SentenceFactory sentenceFactory;
  private final String format;
  private final String serverLocationSentence;
  private final boolean publishRecords;
  private final Map<String, SentenceMapping> registeredSentences;
  private final String destinationName;

  public NMEAProtocol(EndPoint endPoint, Packet packet) throws LoginException, IOException {
    super(endPoint,  endPoint.getConfig().getProtocolConfig("NMEA-0183"));
    if (endPoint instanceof StreamEndPoint) {
      ((StreamEndPoint) endPoint).setStreamHandler(new NMEAStreamHandler());
    }
    if (packet != null) {
      packet.clear();
    }
    SessionContextBuilder sessionContextBuilder = new SessionContextBuilder("NMEA-0183" + endPoint.getId(), new ProtocolClientConnection(this));
    sessionContextBuilder.setSessionExpiry(0);
    sessionContextBuilder.setPersistentSession(false);
    session = SessionManager.getInstance().create(sessionContextBuilder.build(), this);
    selectorTask = new SelectorTask(this, endPoint.getConfig().getEndPointConfig());
    sentenceMap = new LinkedHashMap<>();
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(
        endPoint.getProtocol(),
        endPoint.getName(),
        "NMEA-0183",
        session.getSecurityContext().getUsername()
    );
    setTransformation(transformation);
    ConfigurationProperties configurationProperties = ConfigurationManager.getInstance().getProperties("NMEA-0183");
    format = configurationProperties.getProperty("format", "raw");
    boolean setServerLocation = configurationProperties.getBooleanProperty("serverLocation", false);
    if (setServerLocation) {
      serverLocationSentence = configurationProperties.getProperty("sentenceForPosition");
    } else {
      serverLocationSentence = null;
    }
    publishRecords = configurationProperties.getBooleanProperty("publish", false);
    sentenceFactory = new SentenceFactory((ConfigurationProperties) configurationProperties.get("sentences"));
    registeredSentences = new LinkedHashMap<>();
    destinationName = "$NMEA/" + endPoint.getName();
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException {
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource, @NonNull @NotNull String mappedResource, @Nullable ParserExecutor executor, @Nullable Transformer transformer) {
    registeredSentences.put(resource, new SentenceMapping(mappedResource, transformer));
  }

  @Override
  public void sendMessage(@NotNull @NonNull MessageEvent messageEvent) {
    // This protocol is read only
  }

  @Override
  public Subject getSubject() {
    return session.getSecurityContext().getSubject();
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
        NMEAPacket nmeaPacket = new NMEAPacket(packet);
        String sentenceId = nmeaPacket.getName();
        if (sentenceId.length() == 5) {
          prepareSentence(nmeaPacket.getSentence(), sentenceId, nmeaPacket.getEntries());
        }
      } catch (EndOfBufferException e) {
        packet.position(pos);
      }
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }

  private void prepareSentence(String sentence, String sentenceId, Iterator<String> gpsWords) {
    if (registeredSentences.isEmpty()) {
      publishMessage(sentence, sentenceId, gpsWords, destinationName, null);
    } else {
      SentenceMapping mapping = registeredSentences.get(sentenceId);
      if (mapping != null) {
        publishMessage(sentence, sentenceId, gpsWords, mapping.destination, mapping.transformer);
      } else { // check wild card
        mapping = registeredSentences.get("#");
        if (mapping != null) {
          String destination = mapping.destination;
          if (destination.contains("#")) {
            destination = destination.replace("#", sentenceId);
          }
          publishMessage(sentence, sentenceId, gpsWords, destination, mapping.transformer);
        }
      }
    }
  }

  // findDestination throws an IOException, using computeIfAbsent tends to hide or swallow the exceptions
  @SneakyThrows
  @java.lang.SuppressWarnings({"java:S3824"})
  private void publishMessage(String sentence, String sentenceId, Iterator<String> gpsWords, String destinationName, Transformer transformer) {
    String processed = parseSentence(sentence, sentenceId, gpsWords);
    if (publishRecords) {
      Destination destination = sentenceMap.get(sentenceId);
      if (destination == null) {
        destination = session.findDestination(destinationName, DestinationType.TOPIC).get();
        sentenceMap.put(sentenceId, destination);
      }
      if (destination != null) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(processed.getBytes())
            .setMessageExpiryInterval(8, TimeUnit.SECONDS) // Expire the event in 8 seconds
            .setTransformation(getTransformation());
        if (transformer != null) {
          transformer.transform(messageBuilder);
        }
        Message message = MessageOverrides.createMessageBuilder(protocolConfig.getMessageDefaults(), messageBuilder).build();
        destination.storeMessage(message);
      }
    }
    receivedMessage();
  }

  private String parseSentence(String raw, String sentenceId, Iterator<String> gpsWords) {
    if (format.equalsIgnoreCase("json") || serverLocationSentence != null) {
      Sentence sentence = sentenceFactory.parse(sentenceId, gpsWords);
      if (sentenceId.equalsIgnoreCase(serverLocationSentence)  || sentenceId.toLowerCase().endsWith(serverLocationSentence.toLowerCase())) {
        PositionType latitude = (PositionType) sentence.get("latitude");
        PositionType longitude = (PositionType) sentence.get("longitude");
        LocationManager.getInstance().setPosition(latitude.getPosition(), longitude.getPosition());
      }
      if (sentence != null && format.equalsIgnoreCase("json")) {
        return sentence.toJSON();
      }
    }
    return raw;
  }

  @Override
  public String getName() {
    return "NMEA-0183";
  }

  @Override
  public String getSessionId() {
    return "GPS_" + endPoint.getName();
  }

  @Override
  public String getVersion() {
    return "0183";
  }


  @Data
  @AllArgsConstructor
  private static final class SentenceMapping {
    private final String destination;
    private final Transformer transformer;
  }

  @Override
  public ProtocolInformationDTO getInformation() {
    NmeaProtocolInformation information = new NmeaProtocolInformation();
    updateInformation(information);
    information.setSessionInfo(session.getSessionInformation());
    return information;
  }

}
