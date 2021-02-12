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

package org.maps.network.protocol.impl.amqp.proton.listeners;

import java.util.UUID;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.EventType;
import org.maps.network.protocol.impl.amqp.AMQPProtocol;
import org.maps.network.protocol.impl.amqp.proton.ProtonEngine;

public class ConnectionLocalOpenEventListener extends BaseEventListener {

  public ConnectionLocalOpenEventListener(AMQPProtocol protocol, ProtonEngine engine) {
    super(protocol, engine);
  }

  @Override
  public boolean handleEvent(Event event) {
    Connection conn = event.getConnection();
    if (conn.getRemoteState() == EndpointState.UNINITIALIZED) {
      conn.setContainer(UUID.randomUUID().toString());
    }
    return true;
  }

  @Override
  public EventType getType() {
    return Type.CONNECTION_LOCAL_OPEN;
  }
}
