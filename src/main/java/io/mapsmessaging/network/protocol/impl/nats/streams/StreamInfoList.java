package io.mapsmessaging.network.protocol.impl.nats.streams;

import io.mapsmessaging.engine.destination.DestinationImpl;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StreamInfoList {
  @Getter
  private final String name;
  private final Map<String, StreamInfo> subjects;

  public StreamInfoList(String name) {
    this.name = name;
    subjects = new ConcurrentHashMap<>();
  }

  public List<StreamInfo> getSubjects() {
    return new ArrayList<>(subjects.values());
  }

  public void addSubject(String subject, DestinationImpl destination) {
    StreamInfo info = new StreamInfo(subject, destination);
    subjects.put(subject, info);
  }

  public void removeSubject(String subject) {
    subjects.remove(subject);
  }

  public int size() {
    return subjects.size();
  }
}
