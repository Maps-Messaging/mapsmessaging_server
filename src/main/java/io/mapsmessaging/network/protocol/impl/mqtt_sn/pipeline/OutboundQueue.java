package io.mapsmessaging.network.protocol.impl.mqtt_sn.pipeline;

import io.mapsmessaging.utilities.collections.NaturalOrderedLongList;
import java.util.concurrent.atomic.AtomicLong;

public class OutboundQueue {

  private final NaturalOrderedLongList queueIndex;
  private final AtomicLong indexGenerator;

  public OutboundQueue(){
    queueIndex = new NaturalOrderedLongList();
    indexGenerator = new AtomicLong(0);
  }
}
