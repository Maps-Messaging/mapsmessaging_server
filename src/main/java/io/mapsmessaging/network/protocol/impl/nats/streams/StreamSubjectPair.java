package io.mapsmessaging.network.protocol.impl.nats.streams;

import lombok.Data;

@Data
public class StreamSubjectPair {
  private String stream;
  private String subject;

  public StreamSubjectPair(String stream, String subject) {
    this.stream = stream;
    this.subject = subject.replace('/', '.');
  }
}
