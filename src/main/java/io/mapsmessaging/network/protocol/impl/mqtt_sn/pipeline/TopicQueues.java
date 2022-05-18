package io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline;

import io.mapsmessaging.api.MessageEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class TopicQueues {

  private final String name;
  private final Map<Long, MessageEvent> messageEventMap;

  public TopicQueues(String name){
    this.name = name;
    messageEventMap = new LinkedHashMap<>();
  }

}
