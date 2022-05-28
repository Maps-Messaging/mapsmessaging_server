package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import javax.net.ssl.SSLEngineResult;

public abstract class State {

  protected final StateEngine stateEngine;

  protected State(StateEngine stateEngine){
    this.stateEngine = stateEngine;
  }

  abstract int outbound(Packet packet) throws IOException;

  abstract int inbound(Packet packet) throws IOException;

  void runDelegatedTasks() throws IOException {
    Runnable runnable;
    while ((runnable = stateEngine.getSslEngine().getDelegatedTask()) != null) {
      runnable.run();
    }
    SSLEngineResult.HandshakeStatus hs = stateEngine.getSslEngine().getHandshakeStatus();
    if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
      throw new IOException("handshake shouldn't need additional tasks");
    }
  }


}
