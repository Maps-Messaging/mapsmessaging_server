package io.mapsmessaging.aggregator;

import io.mapsmessaging.api.MessageEvent;

public interface ProcessedHandler {

  void completed(MessageEvent[] contributions);

}
