package io.mapsmessaging.engine.system.impl.load;

import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import io.mapsmessaging.network.io.EndPointServerStatus;
import java.io.IOException;

public class MessageReceived extends SystemTopicWithAverage {

  public MessageReceived() throws IOException {
    super("$SYS/load/messages/received", true);
  }

  @Override
  public long getData() {
    return EndPointServerStatus.SystemTotalPacketsReceived.sum();
  }
}
