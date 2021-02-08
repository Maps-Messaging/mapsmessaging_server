package org.maps.network.protocol.impl.stomp.listener;

import java.io.IOException;
import org.maps.network.protocol.impl.stomp.frames.Frame;
import org.maps.network.protocol.impl.stomp.state.StateEngine;

public class ErrorListener implements FrameListener {

  @Override
  public void frameEvent(Frame frame, StateEngine engine, boolean endOfBuffer) throws IOException {

  }
}
