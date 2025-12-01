/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.impl.serial;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.LockSupport;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING;

public class SerialEndPoint extends EndPoint implements StreamEndPoint {

  private final ExecutorService readExecutor = Executors.newFixedThreadPool(1);
  private final ExecutorService writeExecutor = Executors.newFixedThreadPool(1);

  private final SerialPort serialPort;
  private final OutputStream outputStream;
  private final InputStream inputStream;
  private final EndPointJMX mbean;
  private StreamHandler streamHandler;

  public SerialEndPoint(long id, EndPointServerStatus server, SerialPort serialPort, SerialConfigDTO config, List<String> jmxPath) {
    super(id, server);
    this.serialPort = serialPort;
    name = serialPort.getSystemPortName();
    configure(serialPort, config);
    serialPort.openPort();
    outputStream = serialPort.getOutputStream();
    inputStream = serialPort.getInputStream();
    mbean = new EndPointJMX(jmxPath, this);
    jmxParentPath = mbean.getTypePath();
    streamHandler = new SimpleStreamHandler(config.getBufferSize());
  }

  public static void configure(SerialPort serialPort,  SerialConfigDTO config) {
    serialPort.setBaudRate(config.getBaudRate());
    serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), getStopBits(config), getParity(config));
    serialPort.setFlowControl(config.getFlowControl());
    serialPort.setComPortTimeouts(TIMEOUT_READ_BLOCKING, config.getReadTimeOut(), config.getWriteTimeOut());
  }

  @Override
  public void close() throws IOException {
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
      readExecutor.execute(new SerialReader(runner));
    } else {
      writeExecutor.execute(new SerialWriter(runner));
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
  public StreamHandler getStreamHandler() {
    return streamHandler;
  }

  @Override
  public void setStreamHandler(StreamHandler handler) {
    streamHandler = handler;
  }

  public static int getStopBits(SerialConfigDTO config) {
    return switch (config.getStopBits().toLowerCase()) {
      case "2" -> SerialPort.TWO_STOP_BITS;
      case "1.5" -> SerialPort.ONE_POINT_FIVE_STOP_BITS;
      default -> SerialPort.ONE_STOP_BIT;
    };
  }

  public static int getParity(SerialConfigDTO config) {
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
      while (serialPort.bytesAvailable() == 0) {
        LockSupport.parkNanos(1000000);
      }
      runner.selected(runner, null, SelectionKey.OP_READ);
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
