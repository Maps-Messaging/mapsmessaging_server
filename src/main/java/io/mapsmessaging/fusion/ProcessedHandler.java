package io.mapsmessaging.fusion;

import io.mapsmessaging.api.MessageEvent;

public interface ProcessedHandler {

  void completed(MessageEvent[] contributions);

}
