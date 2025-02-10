package io.mapsmessaging.network.protocol.impl.plugin;


import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.ProtocolConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.connection.EndPointConnection;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import java.util.List;

public class PluginEndPoint extends EndPoint {

  protected PluginEndPoint(long id, EndPointServerStatus server) {
    super(id, server);
  }

  public ProtocolConfigDTO config(){
    List<ProtocolConfigDTO> configured = server.getConfig().getProtocolConfigs();
    if(configured != null){
      return configured.get(0);
    }
    return null;
  }

  @Override
  public String getProtocol() {
    return "plugin";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    return 0;
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) throws IOException {
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) throws ClosedChannelException {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return server.getConfig().getAuthenticationRealm();
  }

  @Override
  public String getName() {
    return "plugin";
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(PluginEndPoint.class);
  }
}
