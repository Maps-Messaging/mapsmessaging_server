package org.maps.network.protocol.impl.loragateway.handler;

import java.util.TreeMap;
import org.maps.network.protocol.impl.loragateway.Constants;

public class DataHandlerFactory {

  private final TreeMap<Byte, PacketHandler> dataHandlers;
  private final PacketHandler unknownHandler;

  public DataHandlerFactory() {
    dataHandlers = new TreeMap<>();
    dataHandlers.put(Constants.DATA, new DataHandler());
    dataHandlers.put(Constants.LOG, new LogHandler());
    dataHandlers.put(Constants.FAILURE, new FailureHandler());
    dataHandlers.put(Constants.SUCCESSFUL, new SuccessHandler());
    dataHandlers.put(Constants.PING, new PingHandler());
    dataHandlers.put(Constants.VERSION, new VersionHandler());
    unknownHandler = new UnknownHandler();
  }

  public PacketHandler getHandler(byte command) {
    PacketHandler handler = dataHandlers.get(command);
    if (handler == null) {
      handler = unknownHandler;
    }
    return handler;
  }
}
