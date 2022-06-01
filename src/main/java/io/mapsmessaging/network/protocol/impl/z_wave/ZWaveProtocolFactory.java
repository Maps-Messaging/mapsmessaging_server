package io.mapsmessaging.network.protocol.impl.z_wave;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import javax.security.auth.login.LoginException;

public class ZWaveProtocolFactory extends ProtocolImplFactory {


  public ZWaveProtocolFactory() {
    super("Z_Wave", "Z_Wave Gateway", new ZWaveProtocolDetection());
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    build(endPoint, packet);
  }

  private ProtocolImpl build(EndPoint endPoint, Packet packet) throws IOException {
    try {
      return new ZWaveProtocol(endPoint, packet);
    } catch (LoginException e) {
    }
    return null;
  }
}
