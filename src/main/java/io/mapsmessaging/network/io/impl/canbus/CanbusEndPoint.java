package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.canbus.device.CanDevice;
import io.mapsmessaging.canbus.device.QueuedCanDevice;
import io.mapsmessaging.canbus.device.SerialCanDevice;
import io.mapsmessaging.canbus.device.SocketCanDevice;
import io.mapsmessaging.canbus.device.codec.impl.WaveshareUsbCanAStreamCodec;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.dto.rest.config.network.impl.CanbusConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServer;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.Selectable;
import io.mapsmessaging.network.io.impl.serial.SerialEndPoint;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortListener;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class CanbusEndPoint extends EndPoint implements SerialPortListener {

  private static final AtomicLong idGenerator = new AtomicLong(0);

  private final AtomicBoolean closed;
  private final EndPointJMX mbean;
  private final CanbusConfigDTO config;
  private final Object deviceLock;

  private volatile CanDevice canDevice;
  private SerialPort activeSerialPort;
  private String activeSerialPortName;

  public CanbusEndPoint(CanbusConfigDTO config, EndPointServer server, List<String> jmxPath) throws IOException {
    super(idGenerator.incrementAndGet(), server);
    this.name = config.getDeviceName();
    this.closed = new AtomicBoolean(false);
    this.config = config;
    this.deviceLock = new Object();

    createDevice();
    if (canDevice != null) {
      name = canDevice.getClass().getName();
    }

    this.mbean = new EndPointJMX(jmxPath, this);
  }

  private void createDevice() throws IOException {
    if (isSerialCanDevice()) {
      registerSerialDevice();
      return;
    }

    synchronized (deviceLock) {
      CanDevice physical = new SocketCanDevice(config.getDeviceName());
      canDevice = new QueuedCanDevice(physical);
    }
  }

  private boolean isSerialCanDevice() {
    return config.getSerialConfig() != null
        && config.getSerialConfig().getSerialDevice() != null;
  }

  private void registerSerialDevice() throws IOException {
    SerialConfigDTO serialConfig = config.getSerialConfig();

    String serialNumber = serialConfig.getSerialDevice().getSerialNo();
    if (serialNumber != null && !serialNumber.isEmpty()) {
      SerialPortScanner.getInstance().addBySerial(serialNumber, this);
      return;
    }

    String portName = serialConfig.getSerialDevice().getPort();
    if (portName != null && !portName.isEmpty()) {
      SerialPort port = SerialPortScanner.getInstance().add(portName, this);
      if(port != null){
        bind(port);
      }
    }
  }

  public InterfaceInformation getInterfaceInformation() {
    CanDevice currentDevice = canDevice;
    if (currentDevice == null) {
      throw new IllegalStateException("CAN bus device is not currently bound");
    }
    return new CanbusInterfaceInfo(currentDevice.getCanCapabilities());
  }

  @Override
  public void close() throws IOException {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    mbean.close();

    CanDevice deviceToClose;
    SerialPort portToClose;

    synchronized (deviceLock) {
      deviceToClose = canDevice;
      portToClose = activeSerialPort;

      canDevice = null;
      activeSerialPort = null;
      activeSerialPortName = null;
    }

    IOException exception = closeDevice(deviceToClose);
    closePort(portToClose);

    if (exception != null) {
      throw exception;
    }
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
    if (closed.get()) {
      throw new IOException("CanbusEndPoint is closed");
    }

    CanDevice currentDevice = canDevice;
    if (currentDevice != null) {
      try {
        return currentDevice.readFrame();
      } catch (EOFException e) {
        if(currentDevice instanceof SerialCanDevice){
          unbind(activeSerialPort);
        }
      }
    }

    try {
      Thread.sleep(100);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IOException(exception);
    }

    return null;
  }

  public void writeFrame(CanFrame frame) throws IOException {
    if (closed.get()) {
      throw new IOException("CanbusEndPoint is closed");
    }

    CanDevice currentDevice = canDevice;
    if (currentDevice != null) {
      currentDevice.writeFrame(frame);
    }
  }

  @Override
  public void bind(SerialPort port) throws IOException {
    if (closed.get()) {
      closePort(port);
      return;
    }

    SerialEndPoint.configure(port, config.getSerialConfig().getSerialDevice());

    if (!port.isOpen() && !port.openPort()) {
      throw new IOException("Failed to open serial CAN port: " + port.getSystemPortName());
    }

    SerialCanDevice newDevice = new SerialCanDevice(
        port.getSystemPortName(),
        port.getInputStream(),
        port.getOutputStream(),
        new WaveshareUsbCanAStreamCodec()
    );

    CanDevice oldDevice;
    SerialPort oldPort;

    synchronized (deviceLock) {
      if (closed.get()) {
        oldDevice = newDevice;
        oldPort = port;
      } else {
        oldDevice = canDevice;
        oldPort = activeSerialPort;

        canDevice = newDevice;
        activeSerialPort = port;
        activeSerialPortName = port.getSystemPortName();
        name = newDevice.getClass().getName();
      }
    }

    closeDeviceQuietly(oldDevice);

    if (oldPort != null && oldPort != port) {
      closePort(oldPort);
    }

    if (closed.get()) {
      closePort(oldPort);
    }
  }

  @Override
  public void unbind(SerialPort port) throws IOException {
    CanDevice deviceToClose = null;
    SerialPort portToClose = null;

    synchronized (deviceLock) {
      String portName = port.getSystemPortName();

      if (activeSerialPortName == null || !activeSerialPortName.equals(portName)) {
        return;
      }

      deviceToClose = canDevice;
      portToClose = activeSerialPort;

      canDevice = null;
      activeSerialPort = null;
      activeSerialPortName = null;
      name = config.getDeviceName();
    }

    IOException exception = closeDevice(deviceToClose);
    closePort(portToClose);

    if (exception != null) {
      throw exception;
    }
  }

  private IOException closeDevice(CanDevice device) {
    if (device == null) {
      return null;
    }

    try {
      device.close();
      return null;
    } catch (IOException exception) {
      return exception;
    }
  }

  private void closeDeviceQuietly(CanDevice device) {
    try {
      closeDevice(device);
    } catch (Exception ignored) {
      // ignored deliberately during hotplug replacement
    }
  }

  private void closePort(SerialPort port) {
    if (port != null && port.isOpen()) {
      port.closePort();
    }
  }
}