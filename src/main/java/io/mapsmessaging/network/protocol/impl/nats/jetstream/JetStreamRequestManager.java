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

package io.mapsmessaging.network.protocol.impl.nats.jetstream;

import io.mapsmessaging.network.protocol.impl.nats.frames.ErrFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.NatsFrame;
import io.mapsmessaging.network.protocol.impl.nats.frames.PayloadFrame;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.JetStreamApiManager;
import io.mapsmessaging.network.protocol.impl.nats.state.SessionState;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JetStreamRequestManager {
  private final JetStreamApiManager jetStreamApiManager;

  private Map<String, String> subscriptionId = new ConcurrentHashMap<>();

  @Getter
  @Setter
  private String jetSubject;

  public JetStreamRequestManager() {
    jetStreamApiManager = new JetStreamApiManager();
  }

  public void close(){
    subscriptionId.clear();
  }

  public void registerSid(String key, String sid){
    subscriptionId.put(key, sid);
  }

  public String getSid(String reply){
    for(Map.Entry<String, String> entry : subscriptionId.entrySet()){
      if(reply.startsWith(entry.getKey())){
        return entry.getValue();
      }
    }
    return "";
  }

  public boolean isJetStreamRequest(PayloadFrame frame) {
    String subject = frame.getSubject();
    return (subject != null && (
        subject.startsWith("$JS")
            || subject.startsWith("$KV")
            || subject.startsWith("$O")));
  }

  public NatsFrame process(PayloadFrame frame, SessionState sessionState) throws IOException {
    String subject = frame.getSubject();

    if (subject.startsWith("$JS.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableStreams()) {
        return new ErrFrame("Streams are disabled");
      }
      return jetStreamApiManager.process(subject, frame, sessionState);
    } else if (subject.startsWith("$KV.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableKeyValues()) {
        return new ErrFrame("Key Values are disabled");
      }
      return new ErrFrame("KeyValue handler not yet implemented");

    } else if (subject.startsWith("$O.")) {
      if (!sessionState.getProtocol().getNatsConfig().isEnableObjectStore()) {
        return new ErrFrame("Object Store is disabled");
      }
      return new ErrFrame("ObjectStore handler not yet implemented");

    } else {
      return new ErrFrame("Unknown JetStream request: " + subject);
    }
  }

}
