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
        MalformedException malformedException =  new MalformedException();
        malformedException.initCause(e);
        throw malformedException;
      }
    }

    return null;
  }
}
