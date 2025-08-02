package io.mapsmessaging.ml.streams;

import io.mapsmessaging.dto.rest.config.ml.MLEventStreamDTO;

import java.util.ArrayList;
import java.util.List;

public class StreamManager {

  private final List<StreamHandler> streamHandlers;

  public StreamManager(List<MLEventStreamDTO> eventStreams) {
    this.streamHandlers = new ArrayList<>();
    for (MLEventStreamDTO eventStream : eventStreams) {
      streamHandlers.add(new StreamHandler(eventStream));
    }
  }

  public void start() {
    streamHandlers.forEach(StreamHandler::start);
  }

  public void stop() {
    streamHandlers.forEach(StreamHandler::stop);
  }
}
