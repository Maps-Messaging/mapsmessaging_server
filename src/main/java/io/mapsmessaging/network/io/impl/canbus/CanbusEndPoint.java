package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.SimpleStreamHandler;
import io.mapsmessaging.network.io.impl.serial.threads.SerialIoExecutors;
import io.mapsmessaging.network.io.impl.serial.threads.SerialIoPoolHandle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING;
import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

public class CanbusEndPoint extends EndPoint {

  private final EndPointJMX mbean;
  private final AtomicBoolean closed;
  private final SocketCanDevice canDevice;
  private final String canbusDeviceName;
  private final ReadCanbusThread readCanbusThread;

  public CanbusEndPoint(long id, EndPointServerStatus server, CanbusConfigDTO config, List<String> jmxPath) throws IOException {
    super(id, server);
    closed = new AtomicBoolean(false);
    mbean = new EndPointJMX(jmxPath, this);
    jmxParentPath = mbean.getTypePath();
    canbusDeviceName = config.getDeviceName();
    canDevice = new SocketCanDevice(config.getDeviceName());
    readCanbusThread = new ReadCanbusThread();
  }

  @Override
  public void close() throws IOException {
    closed.set(true);
    super.close();
    mbean.close();
    canDevice.close();
    server.handleCloseEndPoint(this);
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
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) {
    return null;
  }

  @Override
  public FutureTask<SelectionKey> deregister(int selectionKey) {
    return null;
  }

  @Override
  public String getAuthenticationConfig() {
    return getConfig().getAuthenticationRealm();
  }

  @Override
  protected Logger createLogger() {
    return LoggerFactory.getLogger(SerialEndPoint.class);
  }

  @Override
  public String getRemoteSocketAddress() {
    return canbusDeviceName;
  }


  private final class ReadCanbusThread extends Thread {

    public ReadCanbusThread(){
      setDaemon(true);
      start();
    }

    @Override
    public void run() {
      while(!closed.get()){
        try {
          CanFrame frame = canDevice.readFrame();


        } catch (IOException e) {
          // log closed
          closed.set(true);
        }
      }
    }

  }
}
