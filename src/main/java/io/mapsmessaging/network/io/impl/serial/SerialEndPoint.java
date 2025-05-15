/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.serial;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.config.network.impl.SerialConfig;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.admin.EndPointJMX;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.LockSupport;

public class SerialEndPoint extends EndPoint implements StreamEndPoint {

  private final SerialPort serialPort;
  private final OutputStream outputStream;
  private final InputStream inputStream;
  private final EndPointJMX mbean;
  private StreamHandler streamHandler;

  public SerialEndPoint(long id, EndPointServer server, SerialPort serialPort, EndPointManagerJMX managerMBean) {
    super(id, server);
    SerialConfig config = ((SerialConfig) server.getConfig().getEndPointConfig());
    this.serialPort = serialPort;
    configure(config);
    serialPort.openPort();
    outputStream = serialPort.getOutputStream();
    inputStream = serialPort.getInputStream();
    mbean = new EndPointJMX(managerMBean.getTypePath(), this);
    jmxParentPath = mbean.getTypePath();
    streamHandler = new SimpleStreamHandler(config.getBufferSize());
  }

  private void configure(SerialConfig config) {
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
      SimpleTaskScheduler.getInstance().submit(new SerialReader(runner));
    } else {
      SimpleTaskScheduler.getInstance().submit(new SerialWriter(runner));
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
  public String getName() {
    return serialPort.getSystemPortName();
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

  private int getStopBits(SerialConfig config){
    switch (config.getStopBits().toLowerCase()) {
      case "2":
        return SerialPort.TWO_STOP_BITS;
      case "1.5":
        return SerialPort.ONE_POINT_FIVE_STOP_BITS;
      case "1":
      default:
        return SerialPort.ONE_STOP_BIT;
    }
  }

  private int getParity(SerialConfig config) {
    switch (config.getParity().toLowerCase()) {
      case "o":
        return SerialPort.ODD_PARITY;

      case "e":
        return SerialPort.EVEN_PARITY;

      case "m":
        return SerialPort.MARK_PARITY;

      case "s":
        return SerialPort.SPACE_PARITY;

      case "n":
      default:
        return SerialPort.NO_PARITY;
    }
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
