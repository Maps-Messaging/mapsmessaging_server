package io.mapsmessaging.network.io.impl.canbus;

import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.*;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;


public class CanbusEndPoint extends EndPoint {

  private final AtomicBoolean closed;
  private final SocketCanDevice canDevice;
  private final EndPointJMX mbean;


  public CanbusEndPoint(CanbusConfigDTO config, EndPointServer server,  List<String> jmxPath) throws IOException {
    super(1, server);
    name = config.getDeviceName();
    closed = new AtomicBoolean(false);
    try {
      canDevice = new SocketCanDevice(config.getDeviceName());
    }
    catch(Throwable th){
      throw new IOException(th);
    }
    mbean = new EndPointJMX(jmxPath, this);
  }

  public InterfaceInformation getInterfaceInformation() {
    return new CanbusInterfaceInfo(canDevice.getCanCapabilities());
  }


  public void close() throws IOException {
    mbean.close();
    closed.set(true);
    canDevice.close();
  }

  @Override
  public String getProtocol() {
    return "canbus";
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
    return getConfig().getAuthenticationRealm();
  }


  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(CanbusEndPoint.class);
  }

  @Override
  public String getRemoteSocketAddress() {
    return "";
  }

  public CanFrame readFrame() throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    return  canDevice.readFrame();
  }

  public void writeFrame(CanFrame frame) throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    canDevice.writeFrame(frame);
  }

}
