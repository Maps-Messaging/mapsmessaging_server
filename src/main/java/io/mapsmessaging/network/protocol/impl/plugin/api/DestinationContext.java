package io.mapsmessaging.network.protocol.impl.plugin.api;

import io.mapsmessaging.api.Destination;
import io.mapsmessaging.api.MessageBuilder;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.api.message.TypedData;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DestinationContext {

  private final Destination destination;

  protected DestinationContext(Destination destination) {
    this.destination = destination;
  }

  public int writeEvent(byte[] payload, Map<String, Object> map, ParserExecutor parser) throws IOException {
    Map<String, TypedData> dataMap = new LinkedHashMap<>();
    for(Map.Entry<String, Object> entry:map.entrySet()) {
      dataMap.put(entry.getKey(), new TypedData(entry.getValue()));
    }

    MessageBuilder mb = new MessageBuilder();
    mb.setOpaqueData(payload)
        .setDataMap(dataMap)
        .setCreation(System.currentTimeMillis());
    Message msg = mb.build();
    if(parser == null || parser.evaluate(msg)) {
      return destination.storeMessage(mb.build());
    }
    return 0;
  }

}
