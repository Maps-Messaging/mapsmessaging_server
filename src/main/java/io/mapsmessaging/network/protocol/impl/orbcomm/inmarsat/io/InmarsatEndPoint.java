package io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.io;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.protocol.impl.orbcomm.ogws.data.CommonMessage;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;

public class InmarsatEndPoint extends EndPoint {

  protected InmarsatEndPoint(long id, EndPointServerStatus server) {
    super(id, server);
  }

  public void sendMessage(CommonMessage commonMessage) {

  }

  @Override
  public String getProtocol() {
    return "ogws";
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
    return "";
  }

  @Override
  protected Logger createLogger() {
    return null;
  }

}
