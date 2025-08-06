package io.mapsmessaging.ml.streams;

import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.dto.rest.config.ml.LlmConfigDTO;
import io.mapsmessaging.dto.rest.config.ml.MLEventStreamDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.destination.DestinationManager;
import io.mapsmessaging.engine.destination.DestinationManagerListener;
import io.mapsmessaging.engine.destination.subscription.set.DestinationSet;

import java.util.Map;

public class StreamHandler implements DestinationManagerListener {

  private final MLEventStreamDTO eventStreamConfig;

  public StreamHandler(MLEventStreamDTO eventStreamConfig, LlmConfigDTO llmConfigDTO) {
    this.eventStreamConfig = eventStreamConfig;
  }

  public void start() {
    DestinationManager destinationManager = MessageDaemon.getInstance().getDestinationManager();
    destinationManager.addListener(this);

    String namespace = eventStreamConfig.getTopicFilter();
    Map<String, DestinationImpl> destinationList = destinationManager.get(name -> DestinationSet.matches(namespace, name));
    if(destinationList != null && !destinationList.isEmpty()) {
      destinationList.values().forEach(this::created);
    }
  }

  public void stop() {
    DestinationManager destinationManager = MessageDaemon.getInstance().getDestinationManager();
    destinationManager.removeListener(this);
  }

  @Override
  public void created(DestinationImpl destination) {
    // add new ML filtering to destination
  }

  @Override
  public void deleted(DestinationImpl destination) {
    // clear out any structures related to this destination
  }
}
