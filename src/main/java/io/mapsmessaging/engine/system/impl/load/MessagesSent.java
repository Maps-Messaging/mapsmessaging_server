package io.mapsmessaging.engine.system.impl.load;

import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import io.mapsmessaging.network.io.EndPointServer;
import java.io.IOException;

public class MessagesSent extends SystemTopicWithAverage {

  public MessagesSent() throws IOException {
    super("$SYS/load/messages/sent", true);
  }

  @Override
  public long getData() {
    return EndPointServer.SystemTotalPacketsSent.sum();
  }
}