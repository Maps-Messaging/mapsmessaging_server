package io.mapsmessaging.network.protocol.impl.nats;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.MultiByteArrayDetection;

import java.io.IOException;

public class NatsProtocolFactory extends ProtocolImplFactory {

  private static final byte[][] natsDetect = {"CONNECT ".getBytes(), "INFO ".getBytes()};

  public NatsProtocolFactory() {
    super("NATS", "NATS protocol support as per https://nats.io/documentation/", new MultiByteArrayDetection(natsDetect, 0));
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    NatsProtocol protocol = new NatsProtocol(endPoint);
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new NatsProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }

}
