package io.mapsmessaging.network.protocol.impl.coap;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.InterfaceInformation;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.io.impl.SelectorCallback;
import io.mapsmessaging.network.io.impl.SelectorTask;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.security.auth.login.LoginException;

public class CoapInterfaceManager implements SelectorCallback {

  private final Logger logger;
  private final SelectorTask selectorTask;
  private final EndPoint endPoint;
  private final HashMap<SocketAddress, CoapProtocol> currentSessions;
  private final ProtocolMessageTransformation transformation;

  public CoapInterfaceManager(InterfaceInformation info, EndPoint endPoint) throws IOException {
    logger = LoggerFactory.getLogger("CoAP Protocol on " + endPoint.getName());
    this.endPoint = endPoint;
    currentSessions = new LinkedHashMap<>();
    selectorTask = new SelectorTask(this, endPoint.getConfig().getProperties(), endPoint.isUDP());
    selectorTask.register(SelectionKey.OP_READ);
    transformation = TransformationManager.getInstance().getTransformation(getName(), "<registered>");
  }


  @Override
  public boolean processPacket(Packet packet) throws IOException {
    // OK, we have received a packet, lets find out if we have an existing context for it
    if (packet.getFromAddress() == null) {
      return true; // Ignoring packet since unknown client
    }
    CoapProtocol protocol = currentSessions.computeIfAbsent(packet.getFromAddress(), k -> {
      try {
        return new CoapProtocol(endPoint, this, packet.getFromAddress());
      } catch (LoginException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    });
    if (protocol.getSession().isClosed()) {
      currentSessions.remove(packet.getFromAddress());
      return processPacket(packet);
    }
    return protocol.processPacket(packet);
  }

  @Override
  public void close() {
    for (CoapProtocol protocol : currentSessions.values()) {
      try {
        protocol.close();
      } catch (IOException e) {

      }
      currentSessions.clear();
    }
  }

  @Override
  public String getName() {
    return "CoAP";
  }

  @Override
  public String getSessionId() {
    return "";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public EndPoint getEndPoint() {
    return endPoint;
  }

  public void close(SocketAddress remoteClient) {
    currentSessions.remove(remoteClient);
  }

}