package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.dto.rest.config.network.SerialDeviceDTO;
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

public class CanbusEndPoint extends EndPoint implements StreamEndPoint {

  private final ExecutorService readExecutor;
  private final ExecutorService writeExecutor;

  private final SerialPort serialPort;
  private final OutputStream outputStream;
  private final InputStream inputStream;
  private final EndPointJMX mbean;
  private StreamHandler streamHandler;
  private final AtomicBoolean closed;

  public CanbusEndPoint(long id, EndPointServerStatus server, SerialPort serialPort, SerialConfigDTO config, List<String> jmxPath) {
    super(id, server);
    closed = new AtomicBoolean(false);
    SerialIoPoolHandle pool = SerialIoExecutors.getInstance().acquire(serialPort.getSystemPortName());
    readExecutor = pool.getReadExecutor();
    writeExecutor = pool.getWriteExecutor();

    this.serialPort = serialPort;
    name = serialPort.getSystemPortName();
    configure(serialPort, config.getSerialDevice());
    serialPort.openPort();
    outputStream = serialPort.getOutputStream();
    inputStream = serialPort.getInputStream();
    mbean = new EndPointJMX(jmxPath, this);
    jmxParentPath = mbean.getTypePath();
    streamHandler = new SimpleStreamHandler(config.getSerialDevice().getBufferSize());
  }

  public static void configure(SerialPort serialPort, SerialDeviceDTO config) {
    setupCommPort(serialPort, config);
    serialPort.setComPortTimeouts(TIMEOUT_READ_BLOCKING | TIMEOUT_WRITE_BLOCKING, config.getReadTimeOut(), config.getWriteTimeOut());
  }

  @Override
  public void close() throws IOException {
    closed.set(true);
    super.close();
    mbean.close();
    serialPort.closePort();
    server.handleCloseEndPoint(this);
  }

  @Override
  public String getProtocol() {
    return "serial";
  }

  @Override
  public int sendPacket(Packet packet) throws IOException {
    int wrote = streamHandler.parseOutput(outputStream, packet);
    updateWriteBytes(wrote);
    return wrote;
  }

  @Override
  public int readPacket(Packet packet) throws IOException {
    try {
      int read = streamHandler.parseInput(inputStream, packet);
      updateReadBytes(read);
      return read;
    } catch (IOException e) {
      close();
      throw e;
    }
  }

  @Override
  public FutureTask<SelectionKey> register(int selectionKey, Selectable runner) {
    if (selectionKey == SelectionKey.OP_READ) {
      readExecutor.execute(new CanbusEndPoint.SerialReader(runner));
    } else {
      writeExecutor.execute(new CanbusEndPoint.SerialWriter(runner));
    }
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
    return serialPort.getSystemPortName();
  }

  @Override
  public StreamHandler getStreamHandler() {
    return streamHandler;
  }

  @Override
  public void setStreamHandler(StreamHandler handler) {
    streamHandler = handler;
  }

  public static void setupCommPort(SerialPort serialPort, SerialDeviceDTO config) {
    serialPort.setBaudRate(config.getBaudRate());
    serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), getStopBits(config), getParity(config));
    serialPort.setFlowControl(config.getFlowControl());
  }

  public static int getStopBits(SerialDeviceDTO config) {
    double stopBits = config.getStopBits();

    if (Double.compare(stopBits, 1.0) == 0) {
      return SerialPort.ONE_STOP_BIT;
    }
    if (Double.compare(stopBits, 2.0) == 0) {
      return SerialPort.TWO_STOP_BITS;
    }
    if (Double.compare(stopBits, 1.5) == 0) {
      return SerialPort.ONE_POINT_FIVE_STOP_BITS;
    }
    return SerialPort.ONE_STOP_BIT;
  }

  public static int getParity(SerialDeviceDTO config) {
    return switch (config.getParity().toLowerCase()) {
      case "o" -> SerialPort.ODD_PARITY;
      case "e" -> SerialPort.EVEN_PARITY;
      case "m" -> SerialPort.MARK_PARITY;
      case "s" -> SerialPort.SPACE_PARITY;
      default -> SerialPort.NO_PARITY;
    };
  }

  //<editor-fold desc="Serial Reader Thread task">
  public class SerialReader implements Runnable {

    private final Selectable runner;

    public SerialReader(Selectable selectable) {
      runner = selectable;
    }

    public void run() {
      long delayNanos = 1_000_000L; // 1 ms
      long maxDelayNanos = 100_000_000L; // 100 ms

      while (!closed.get()) {
        int available = serialPort.bytesAvailable();
        if (available > 0) {
          runner.selected(runner, null, SelectionKey.OP_READ);
          return;
        }

        LockSupport.parkNanos(delayNanos);

        if (delayNanos < maxDelayNanos) {
          delayNanos = Math.min(maxDelayNanos, delayNanos * 2);
        }
      }
    }
  }

  //</editor-fold>

  //<editor-fold desc="Serial Write Thread task">
  public static class SerialWriter implements Runnable {

    private final Selectable runner;

    public SerialWriter(Selectable selectable) {
      runner = selectable;
    }

    public void run() {
      runner.selected(runner, null, SelectionKey.OP_WRITE);
    }
  }
  //</editor-fold>

}
