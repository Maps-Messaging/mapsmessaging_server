package io.mapsmessaging.engine.system.impl.load;

import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import io.mapsmessaging.network.io.EndPointServerStatus;
import java.io.IOException;

public class MessagesSent extends SystemTopicWithAverage {

  public MessagesSent() throws IOException {
    super("$SYS/load/messages/sent", true);
  }

  @Override
  public long getData() {
    return EndPointServerStatus.SystemTotalPacketsSent.sum();
  }
}