package io.mapsmessaging.ml.streams;

import io.mapsmessaging.config.ml.MLModelManagerConfig;
import io.mapsmessaging.dto.rest.config.ml.MLEventStreamDTO;

import java.util.ArrayList;
import java.util.List;

public class StreamManager {

  private final List<StreamHandler> streamHandlers;

  public StreamManager(MLModelManagerConfig mlModelManagerConfig) {
    this.streamHandlers = new ArrayList<>();
    for (MLEventStreamDTO eventStream : mlModelManagerConfig.getEventStreams()) {
      streamHandlers.add(new StreamHandler(eventStream, mlModelManagerConfig.getLlmConfig()));
    }
  }

  public void start() {
    streamHandlers.forEach(StreamHandler::start);
  }

  public void stop() {
    streamHandlers.forEach(StreamHandler::stop);
  }
}
