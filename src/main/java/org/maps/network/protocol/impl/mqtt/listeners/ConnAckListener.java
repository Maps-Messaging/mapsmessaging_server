package org.maps.network.protocol.impl.mqtt.listeners;

import java.io.IOException;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.packet.MQTTPacket;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;

public class ConnAckListener extends BaseConnectionListener {

  @Override
  public MQTTPacket handlePacket(MQTTPacket mqttPacket, Session session, EndPoint endPoint, ProtocolImpl protocol) throws MalformedException {

    String sess = protocol.getEndPoint().getConfig().getProperties().getProperty("sessionId");
    String user = protocol.getEndPoint().getConfig().getProperties().getProperty("username");
    String pass = protocol.getEndPoint().getConfig().getProperties().getProperty("password");

    SessionContextBuilder scb = getBuilder(endPoint, protocol, sess, false, 30000, user, pass.toCharArray());
    protocol.setKeepAlive(30000);
    try {
      Session session1 = createSession(endPoint, protocol, scb, sess);
      session1.resumeState();
      protocol.setConnected(true);
    } catch (IOException ioException) {
      try {
        endPoint.close();
        protocol.setConnected(false);
      } catch (IOException e) {
      }
    }

    return null;
  }
}
