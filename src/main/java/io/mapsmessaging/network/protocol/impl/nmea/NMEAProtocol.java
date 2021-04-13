/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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
 */

package io.mapsmessaging.network.protocol.impl.nmea;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionContextBuilder;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.api.SubscribedEventManager;
import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.location.LocationManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.StreamEndPoint;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.EndOfBufferException;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.Sentence;
import io.mapsmessaging.network.protocol.impl.nmea.sentences.SentenceFactory;
import io.mapsmessaging.network.protocol.impl.nmea.types.PositionType;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NMEAProtocol extends ProtocolImpl {

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
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties());
    sentenceMap = new LinkedHashMap<>();
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    setTransformation(TransformationManager.getInstance().getTransformation(getName(), null));
    ConfigurationProperties configurationProperties = ConfigurationManager.getInstance().getProperties("nmea");
    format = configurationProperties.getProperty("format", "raw");
    boolean setServerLocation = configurationProperties.getBooleanProperty("serverLocation", false);
    if(setServerLocation){
      serverLocationSentence = configurationProperties.getProperty("sentenceForPosition");
    }
    else{
      serverLocationSentence = null;
    }
    publishRecords = configurationProperties.getBooleanProperty("publish", false);
    sentenceFactory = new SentenceFactory((ConfigurationProperties) configurationProperties.get("sentences"));
    registeredSentences = new LinkedHashMap<>();
    destinationName = "$NMEA/"+endPoint.getName();
    setConnected(true);
  }

  @Override
  public void connect(String sessionId, String username, String password) throws IOException{
    completedConnection();
  }

  @Override
  public void subscribeRemote(@NonNull @NotNull String resource,@NonNull @NotNull String mappedResource, @Nullable Transformer transformer){
    registeredSentences.put(resource, new SentenceMapping(mappedResource, transformer));
  }

  @Override
  public void sendMessage(@NonNull @NotNull Destination destination, @NonNull @NotNull String normalisedName, @NonNull @NotNull SubscribedEventManager subscription, @NonNull @NotNull Message message, @NonNull @NotNull Runnable completionTask) {
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
        if(sentence.startsWith("$")){
          sentence = removeFraming(sentence);
        }
        Iterator<String> gpsWords = new ArrayList<>(Arrays.asList(sentence.split(","))).iterator();
        String sentenceId = gpsWords.next();
        if(sentenceId.length() == 5){
          prepareSentence(sentence, sentenceId, gpsWords);
        }
      } catch (EndOfBufferException e) {
        packet.position(pos);
      }
    }
    endPoint.register(SelectionKey.OP_READ, selectorTask.getReadTask());
    return true;
  }

  private void prepareSentence(String sentence, String sentenceId, Iterator<String> gpsWords) throws IOException {
    if(registeredSentences.isEmpty()){
      publishMessage(sentence, sentenceId, gpsWords, destinationName, null);
    }
    else{
      SentenceMapping mapping = registeredSentences.get(sentenceId);
      if(mapping != null){
        publishMessage(sentence, sentenceId, gpsWords, mapping.destination, mapping.transformer);
      }
      else{ // check wild card
        mapping = registeredSentences.get("#");
        if(mapping != null){
          String destination = mapping.destination;
          if(destination.contains("#")){
            destination = destination.replace("#", sentenceId);
          }
          publishMessage(sentence, sentenceId, gpsWords, destination, mapping.transformer);
        }
      }
    }
  }

  // findDestination throws an IOException, using computeIfAbsent tends to hide or swallow the exceptions
  @java.lang.SuppressWarnings({"java:S3824"})
  private void publishMessage(String sentence, String sentenceId, Iterator<String> gpsWords, String destinationName, Transformer transformer) throws IOException {
    String processed = parseSentence(sentence, sentenceId, gpsWords);
    if(publishRecords) {
      Destination destination = sentenceMap.get(sentenceId);
      if (destination == null) {
        destination = session.findDestination(destinationName);
        sentenceMap.put(sentenceId, destination);
      }
      if (destination != null) {
        MessageBuilder messageBuilder = new MessageBuilder();
        messageBuilder.setOpaqueData(processed.getBytes())
            .setRetain(false)
            .storeOffline(false)
            .setMessageExpiryInterval(8, TimeUnit.SECONDS) // Expire the event in 8 seconds
            .setQoS(QualityOfService.AT_MOST_ONCE)
            .setTransformation(getTransformation());

        if (transformer != null) {
          transformer.transform(messageBuilder);
        }
        destination.storeMessage(messageBuilder.build());
      }
    }
    receivedMessage();
  }

  private String parseSentence(String raw, String sentenceId, Iterator<String> gpsWords){
    if(format.equalsIgnoreCase("json") || serverLocationSentence != null) {
      Sentence sentence = sentenceFactory.parse(sentenceId, gpsWords);
      if(serverLocationSentence.equalsIgnoreCase(sentenceId)){
        PositionType latitude = (PositionType) sentence.get("latitude");
        PositionType longitude = (PositionType) sentence.get("longitude");
        LocationManager.getInstance().setPosition(latitude.getPosition(), longitude.getPosition());
      }
      if(sentence != null && format.equalsIgnoreCase("json")) {
        return sentence.toJSON();
      }
    }
    return raw;
  }

  private String removeFraming(String sentence) {
    sentence = sentence.substring(1);
    sentence = sentence.substring(0, sentence.indexOf("*"));
    return sentence;
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
    return "0183";
  }


  private static final class SentenceMapping{

    private final String destination;
    private final Transformer transformer;

    public SentenceMapping(String destination, Transformer transformer) {
      this.destination = destination;
      this.transformer = transformer;
    }
  }

}
