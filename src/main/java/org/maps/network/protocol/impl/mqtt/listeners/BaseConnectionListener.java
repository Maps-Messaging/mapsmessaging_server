package org.maps.network.protocol.impl.mqtt.listeners;

import java.io.IOException;
import javax.security.auth.login.LoginException;
import org.maps.logging.LogMessages;
import org.maps.messaging.api.Session;
import org.maps.messaging.api.SessionContextBuilder;
import org.maps.messaging.api.SessionManager;
import org.maps.network.io.EndPoint;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.impl.mqtt.DefaultConstants;
import org.maps.network.protocol.impl.mqtt.MQTTProtocol;
import org.maps.network.protocol.impl.mqtt.packet.MalformedException;
import org.maps.network.protocol.transformation.TransformationManager;

public abstract class BaseConnectionListener extends PacketListener {

  protected Session createSession(EndPoint endPoint, ProtocolImpl protocol, SessionContextBuilder scb, String sessionId) throws MalformedException, IOException {
    Session session;
    try {
      session = SessionManager.getInstance().create(scb.build(), protocol);
      ((MQTTProtocol) protocol).setSession(session);
    } catch (LoginException e) {
      logger.log(LogMessages.MQTT_CONNECT_LISTENER_SESSION_EXCEPTION, e, sessionId);
      endPoint.close();
      throw new MalformedException("[MQTT-3.1.0-2] Failed to create the session for the MQTT session");
    } catch (IOException ioe) {
      logger.log(LogMessages.MQTT_CONNECT_LISTENER_SESSION_EXCEPTION, ioe, sessionId);
      endPoint.close();
      throw new MalformedException("Unable to construct the required Will Topic");
    }
    session.login();
    protocol.setTransformation(TransformationManager.getInstance().getTransformation(protocol.getName(), session.getSecurityContext().getUsername()));
    return session;
  }

  protected SessionContextBuilder getBuilder(EndPoint endPoint, ProtocolImpl protocol, String sessionId, boolean isClean, int keepAlive, String username, char[] pass){
    SessionContextBuilder scb = new SessionContextBuilder(sessionId, protocol);
    scb.setPersistentSession(true)
        .setResetState(isClean)
        .setKeepAlive(keepAlive)
        .setSessionExpiry(endPoint.getConfig().getProperties().getIntProperty("maximumSessionExpiry", DefaultConstants.SESSION_TIME_OUT));

    if (pass != null && pass.length > 0) {
      scb.setPassword(pass);
    }
    if (username != null && username.length() > 0) {
      scb.setUsername(username);
    }
    scb.setReceiveMaximum(DefaultConstants.RECEIVE_MAXIMUM);
    return scb;
  }

}
