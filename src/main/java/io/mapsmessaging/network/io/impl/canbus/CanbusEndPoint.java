package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.SerialCanDevice;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.codec.impl.WaveshareUsbCanAStreamCodec;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortListener;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


public class CanbusEndPoint extends EndPoint implements SerialPortListener{

  private static final AtomicLong idGenerator = new AtomicLong(0);
  private final AtomicBoolean closed;
  private final EndPointJMX mbean;
  private final CanbusConfigDTO config;

  private final AtomicBoolean isBound = new AtomicBoolean(false);
  private CanDevice canDevice;


  public CanbusEndPoint(CanbusConfigDTO config, EndPointServer server, List<String> jmxPath) throws IOException {
    super(idGenerator.incrementAndGet(), server);
    this.name = config.getDeviceName();
    this.closed = new AtomicBoolean(false);
    this.config = config;
    try {
      this.canDevice = createDevice();
    }
    catch (Throwable th) {
      throw new IOException(th);
    }
    name = canDevice.getClass().getName();
    this.mbean = new EndPointJMX(jmxPath, this);
  }

  private CanDevice createDevice() throws IOException {
    if(config.getSerialConfig() != null && config.getSerialConfig().getSerialDevice() != null){
      SerialPort port = null;
      SerialConfigDTO serialConfig = config.getSerialConfig();
      String serialNo = serialConfig.getSerialDevice().getSerialNo();
      if(serialNo != null && !serialNo.isEmpty()){
        port = SerialPortScanner.getInstance().addBySerial(serialNo, this);
      }
      else if(serialConfig.getSerialDevice().getPort() != null && !serialConfig.getSerialDevice().getPort().isEmpty()){
        port = SerialPortScanner.getInstance().add(serialConfig.getSerialDevice().getPort(), this);
      }
      if (port != null) {
        bind(port);
        return canDevice;
      }
      return null;
    }
    else {
      isBound.set(true);
      return new SocketCanDevice(config.getDeviceName());
    }
  }

  public InterfaceInformation getInterfaceInformation() {
    return new CanbusInterfaceInfo(canDevice.getCanCapabilities());
  }

  @Override
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
    CanDevice device = null;
    synchronized (isBound) {
      if(isBound.get()){
        device = canDevice;
      }
    }
    if(device != null){
      return  device.readFrame();
    }
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException(e);
    }
    return null;
  }

  public void writeFrame(CanFrame frame) throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    CanDevice device = null;

    synchronized (isBound) {
      if(isBound.get()){
        device = canDevice;
      }
    }
    if(device != null){
      device.writeFrame(frame);
    }
  }

  @Override
  public void bind(SerialPort port) throws IOException {
    SerialEndPoint.configure(port,config.getSerialConfig().getSerialDevice());
    port.openPort();
    synchronized (isBound) {
      canDevice = new SerialCanDevice(port.getSystemPortName(), port.getInputStream(), port.getOutputStream(), new WaveshareUsbCanAStreamCodec());
      isBound.set(true);
    }
  }

  @Override
  public void unbind(SerialPort port) throws IOException {
    synchronized (isBound) {
      isBound.set(false);
      canDevice = null;
    }
  }
}
