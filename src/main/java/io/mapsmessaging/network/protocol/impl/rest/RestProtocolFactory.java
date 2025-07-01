package io.mapsmessaging.network.protocol.impl.rest;

import io.mapsmessaging.dto.rest.config.protocol.impl.ExtensionConfigDTO;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.detection.NoOpDetection;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionEndPoint;
import io.mapsmessaging.network.protocol.impl.extension.ExtensionProtocol;

import java.io.IOException;

public class RestProtocolFactory extends ProtocolImplFactory {

  public RestProtocolFactory() {
    super("rest", "Provides a REST web hooks or web target", new NoOpDetection());
  }

  @Override
  public Protocol connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    ExtensionConfigDTO protocolConfigDTO = (ExtensionConfigDTO) ((ExtensionEndPoint) endPoint).config();
    Protocol protocol = new ExtensionProtocol(endPoint, new RestProtocol(endPoint, protocolConfigDTO));
    protocol.connect(sessionId, username, password);
    return protocol;
  }

  @Override
  public void create(EndPoint endPoint, Packet packet) throws IOException {
    // We don't accept incoming pulsar client connections
  }

  @Override
  public String getTransportType() {
    return "";
  }
}
