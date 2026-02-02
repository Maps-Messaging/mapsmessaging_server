package io.mapsmessaging.network.io.impl.canbus;

import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.config.network.impl.SerialConfigDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.admin.EndPointManagerJMX;
import io.mapsmessaging.network.io.*;
import io.mapsmessaging.network.io.impl.Selector;
import io.mapsmessaging.network.io.impl.serial.SerialEndPointServer;
import io.mapsmessaging.network.io.impl.serial.management.SerialPortScanner;

import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.utilities.threads.SimpleTaskScheduler;

import java.io.IOException;

import java.util.concurrent.TimeUnit;

public class CanbusEndpointServer extends EndPointServer {

  private final EndPointManagerJMX managerMBean;
  private final ProtocolFactory protocolFactory;
  private CanbusEndPoint canbusEndPoint;
  private final SerialConfigDTO serialConfig;

  public CanbusEndpointServer(AcceptHandler acceptHandler, EndPointURL url, EndPointServerConfigDTO config, EndPointManagerJMX managerMBean) {
    super(acceptHandler, url, config);
    serialConfig = (SerialConfigDTO)config.getEndPointConfig();
    protocolFactory = new ProtocolFactory(config.getProtocols());
    this.managerMBean = managerMBean;
    canbusEndPoint = null;
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
        logger.log(ServerLogMessages.SERIAL_SERVER_CREATE_EXCEPTION, e);
      }
    }
  }

  @Override
  public String getName() {
    return "canbus" + serialConfig.getSerialDevice().getPort();
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
    canbusEndPoint = null;
  }

  @Override
  public void start() throws IOException {

  }

  @Override
  protected Logger createLogger(String url) {
    return LoggerFactory.getLogger(SerialEndPointServer.class);
  }

  @Override
  public void close() {
    SerialPortScanner.getInstance().del(serialConfig.getSerialDevice().getPort());
    if (canbusEndPoint != null) {
      try {
        canbusEndPoint.close();
      } catch (IOException e) {
        logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
      }
    }
  }

  @Override
  public void selected(Selectable selectable, Selector selector, int selection) {
    // Nothing to select here

  }

  public void bind(SerialPort serialPort) throws IOException {
    canbusEndPoint = new CanbusEndPoint(generateID(), this, serialPort, serialConfig, managerMBean.getTypePath());
    handleNewEndPoint(canbusEndPoint);
  }

  public void unbind(SerialPort port) throws IOException {
    if (canbusEndPoint != null && port.getSystemPortName().equals(canbusEndPoint.getName())) {
      canbusEndPoint.close();
      SimpleTaskScheduler.getInstance().schedule(() -> {
        try {
          start();
        } catch (IOException e) {
          logger.log(ServerLogMessages.END_POINT_CLOSE_EXCEPTION, e);
        }
      }, 2, TimeUnit.SECONDS);
    }
  }
}