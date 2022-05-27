package io.mapsmessaging.network.protocol.impl.semtech;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SemTechProtocolFactory extends ProtocolImplFactory {

  private final Map<EndPoint, SemTechProtocol> mappedInterfaces;

  public SemTechProtocolFactory() {
    super("semtech", "SemTech UDP protocol", null);
    mappedInterfaces = new ConcurrentHashMap<>();
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {

  }

  @Override
  public void create(EndPoint endPoint, InterfaceInformation info) throws IOException {
    SemTechProtocol protocol = new SemTechProtocol(endPoint);
    mappedInterfaces.put(endPoint, protocol);
  }
}
