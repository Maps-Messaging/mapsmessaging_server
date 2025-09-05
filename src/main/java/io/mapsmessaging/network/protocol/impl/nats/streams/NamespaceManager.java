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

package io.mapsmessaging.network.protocol.impl.nats.streams;


import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamConfig;
import io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.api.handlers.data.StreamEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class NamespaceManager implements DestinationManagerListener {

  private final Map<String, StreamInfoList> streams;
  private final AtomicReference<StreamEntryManager> streamEntryManager;

  private NamespaceManager() {
    streamEntryManager = new AtomicReference<>();
    streams = new ConcurrentHashMap<>();
    Map<String, DestinationImpl> existing = MessageDaemon.getInstance().getSubSystemManager().getDestinationManager().get(name -> true);
    for (DestinationImpl destination : existing.values()) {
      created(destination);
    }
    MessageDaemon.getInstance().getSubSystemManager().getDestinationManager().addListener(this);
  }

  public static NamespaceManager getInstance() {
    return Holder.INSTANCE;
  }

  @Override
  public void created(DestinationImpl destination) {
    String namespace = destination.getFullyQualifiedNamespace();
    if (namespace.startsWith("$")) return;
    if (namespace.contains("/")) {
      String trimmed = namespace.startsWith("/") ? namespace.substring(1) : namespace;
      int firstSlash = trimmed.indexOf('/');
      if (firstSlash != -1) {
        StreamSubjectPair pair = new StreamSubjectPair(trimmed.substring(0, firstSlash), trimmed.substring(firstSlash + 1));
        StreamInfoList streamInfo = streams.computeIfAbsent(pair.getStream(), k -> new StreamInfoList(pair.getStream()));
        streamInfo.addSubject(pair.getSubject(), destination);
        streamEntryManager.set(null);
      }
    }
  }

  @Override
  public void deleted(DestinationImpl destination) {
    String namespace = destination.getFullyQualifiedNamespace();
    if (namespace.contains("/")) {
      String trimmed = namespace.startsWith("/") ? namespace.substring(1) : namespace;
      int firstSlash = trimmed.indexOf('/');
      if (firstSlash != -1) {
        StreamSubjectPair pair = new StreamSubjectPair(trimmed.substring(0, firstSlash), trimmed.substring(firstSlash + 1));
        StreamInfoList streamInfo = streams.get(pair.getStream());
        if (streamInfo != null) {
          streamInfo.removeSubject(pair.getSubject());
          if (streamInfo.size() == 0) {
            streams.remove(pair.getStream());
          }
        }
        streamEntryManager.set(null);
      }
    }
  }

  public List<StreamEntry> getStreamEntries() {
    StreamEntryManager tmp = streamEntryManager.get();
    if (tmp == null) {
      tmp = new StreamEntryManager();
      tmp.list = new ArrayList<>();
      for (Map.Entry<String, StreamInfoList> entry : streams.entrySet()) {
        StreamInfoList streamInfo = entry.getValue();
        StreamConfig streamConfig = new StreamConfig();
        Date created = new Date(System.currentTimeMillis());
        List<String> subjects = new ArrayList<>();
        boolean isFile = false;
        for (StreamInfo info : streamInfo.getSubjects()) {
          DestinationImpl destination = info.getDestination();
          Date destDate = destination.getResourceProperties().getDate();
          if (destDate != null && destDate.before(created)) {
            created = destDate;
          }
          isFile = isFile || destination.isPersistent();
          subjects.add(info.getSubject());
        }
        streamConfig.setName(entry.getKey());
        streamConfig.setSubjects(subjects);
        streamConfig.setStorage(isFile ? "file" : "memory");
        StreamEntry streamEntry = new StreamEntry();
        streamEntry.setConfig(streamConfig);
        streamEntry.setCreated(created.toInstant());
        tmp.list.add(streamEntry);
      }
      streamEntryManager.set(tmp);
    }
    return tmp.list;
  }

  public StreamInfoList getStream(final String streamName) {
    return streams.get(streamName);
  }

  private static class Holder {
    static final NamespaceManager INSTANCE = new NamespaceManager();
  }

  private static class StreamEntryManager {
    private List<StreamEntry> list;
  }
}
