package io.mapsmessaging.network.io.impl.dtls.state;

import io.mapsmessaging.network.io.Packet;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

public class NormalState extends State {

  public NormalState(StateEngine stateEngine){
    super(stateEngine);
  }

  @Override
  int outbound(Packet packet) throws IOException {
    ByteBuffer appNet = ByteBuffer.allocate(32768);
    SSLEngineResult r = stateEngine.getSslEngine().wrap(packet.getRawBuffer(), appNet);
    appNet.flip();
    Packet p = new Packet(appNet);
    p.setFromAddress(stateEngine.getClientId());
    return stateEngine.send(p);
  }

  @Override
  int inbound(Packet packet) throws SSLException {
    Packet networkOut = new Packet(2048, false);
    SSLEngineResult rs = stateEngine.getSslEngine().unwrap(packet.getRawBuffer(), networkOut.getRawBuffer());
    if(rs.getStatus() == Status.OK) {
      networkOut.flip();
      networkOut.setFromAddress(packet.getFromAddress());
      stateEngine.pushToInBoundQueue(networkOut);
    }
    return packet.position();
  }
}
