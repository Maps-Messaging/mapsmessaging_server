package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.SerialCanDevice;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.codec.impl.WaveshareUsbCanAStreamCodec;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.config.network.SerialDeviceHelper;
import io.mapsmessaging.devices.serial.SerialBusManager;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
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


public class CanbusEndPoint extends EndPoint {

  private final AtomicBoolean closed;
  private final CanDevice canDevice;
  private final EndPointJMX mbean;


  public CanbusEndPoint(CanbusConfigDTO config, EndPointServer server, List<String> jmxPath) throws IOException {
    super(1, server);
    this.name = config.getDeviceName();
    this.closed = new AtomicBoolean(false);

    try {
      this.canDevice = createDevice(config);
    }
    catch (Throwable th) {
      throw new IOException(th);
    }
    name = canDevice.getClass().getName();
    this.mbean = new EndPointJMX(jmxPath, this);
  }

  private CanDevice createDevice(CanbusConfigDTO config) throws IOException {
    if(config.getSerialConfig() != null && config.getSerialConfig().getSerialDevice() != null){
      SerialPort port = null;
      int count = 0;
      while(port == null && count < 100) {
        port = SerialPortScanner.getInstance().allocatePort(config.getSerialConfig().getSerialDevice().getPort());
        if(port == null) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {

          }
        }
        count++;
      }
      if(port == null) throw new IOException("Failed to allocate serial port after multiple attempts");
      SerialEndPoint.configure(port, config.getSerialConfig().getSerialDevice());
      port.openPort();
      return new SerialCanDevice(port.getSystemPortName(), port.getInputStream(), port.getOutputStream(), new WaveshareUsbCanAStreamCodec());
    }
    else {
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
    return  canDevice.readFrame();
  }

  public void writeFrame(CanFrame frame) throws IOException {
    if( closed.get()){
      throw new IOException("CanbusEndPoint is closed");
    }
    canDevice.writeFrame(frame);
  }
}
