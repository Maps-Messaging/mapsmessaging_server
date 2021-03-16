/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.network.io.impl.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.maps.logging.LogMessages;
import org.maps.logging.Logger;
import org.maps.logging.LoggerFactory;
import org.maps.network.EndPointURL;
import org.maps.network.NetworkConfig;
import org.maps.network.SerialEndPointURL;
import org.maps.network.admin.EndPointManagerJMX;
import org.maps.network.io.AcceptHandler;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointServer;
import org.maps.network.io.Packet;
import org.maps.network.io.Selectable;
import org.maps.network.io.impl.Selector;
import org.maps.network.protocol.ProtocolFactory;
import org.maps.network.protocol.ProtocolImplFactory;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class SerialEndPointServer extends EndPointServer {

  private final EndPointManagerJMX managerMBean;
  private final ProtocolFactory protocolFactory;
  private final SerialEndPointURL serialEndPointURL;
  private SerialEndPoint serialEndPoint;

  public SerialEndPointServer(AcceptHandler acceptHandler, EndPointURL url, NetworkConfig config,  EndPointManagerJMX managerMBean) {
    super(acceptHandler, url, config);
    serialEndPointURL = (SerialEndPointURL)url;
    protocolFactory = new ProtocolFactory(config.getProtocols());
    this.managerMBean = managerMBean;
    serialEndPoint = null;
  }

  @Override
  public void handleNewEndPoint(EndPoint endPoint) throws IOException {
    activeEndPoints.remove(endPoint.getId());
    activeEndPoints.put(endPoint.getId(), endPoint);

    //
    // Since this is a serial connection and if it is bound to 1 protocol simply pass it to the protocol
    // else we will need to call the default protocol accept to figure out which one it is
    //
    String protocols = getConfig().getProtocols();
    if (protocols.contains(",")) {
      // We have a list
      acceptHandler.accept(endPoint);
    } else {
      ProtocolImplFactory factory = protocolFactory.getBoundedProtocol();
      try {
        Packet packet = new Packet(10, false);
        packet.flip();
        factory.create(endPoint, packet);
      } catch (IOException e) {
        logger.log(LogMessages.SERIAL_SERVER_CREATE_EXCEPTION, e);
      }
    }
  }

  @Override
  public String getName() {
    return "serial_"+serialEndPointURL.getPortName();
  }

  @Override
  public void register() {
    // Nothing to register here
  }

  @Override
  public void deregister() {
    // Nothing to deregister here
  }

  @Override
  public void handleCloseEndPoint(EndPoint endPoint) {
    super.handleCloseEndPoint(endPoint);
    serialEndPoint = null;
    SerialEndPortScanner.getInstance().del(serialEndPointURL.getPortName());
    SimpleTaskScheduler.getInstance().schedule(() -> {
      try {
        start();
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }, 5, TimeUnit.SECONDS);
  }

  @Override
  public void start() throws IOException {
    SerialPort port = SerialEndPortScanner.getInstance().add(serialEndPointURL.getPortName(), this);
    if (port != null) {
      bind(port);
    }
  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SerialEndPointServer.class);
  }

  @Override
  public void close() {
    SerialEndPortScanner.getInstance().del(serialEndPointURL.getPortName());
    if (serialEndPoint != null) {
      try {
        serialEndPoint.close();
      } catch (IOException e) {
        logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Nothing to select here

  }

  public void bind(SerialPort port) throws IOException {
    port.setBaudRate(serialEndPointURL.getBaudRate());
    port.setComPortParameters(serialEndPointURL.getBaudRate(), serialEndPointURL.getData(), serialEndPointURL.getStop(), serialEndPointURL.getParity());
    serialEndPoint = new SerialEndPoint(generateID(), this, port, managerMBean);
    handleNewEndPoint(serialEndPoint);
  }

  public void unbind(SerialPort port) throws IOException {
    if (serialEndPoint != null && port.getSystemPortName().equals(serialEndPoint.getName())) {
      serialEndPoint.close();
      SimpleTaskScheduler.getInstance().schedule(() -> {
        try {
          start();
        } catch (IOException e) {
          logger.log(LogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      }, 2, TimeUnit.SECONDS);
    }
  }
}
