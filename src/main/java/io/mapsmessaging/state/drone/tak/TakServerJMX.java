/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.state.drone.tak;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import io.mapsmessaging.utilities.admin.JMXManager;

import javax.management.ObjectInstance;
import java.util.List;

/**
 * Health of one shared TAK server connection, registered as
 * {@code io.mapsmessaging:type=Integration,name=TakServer,server=<host>_<port>}.
 */
@JMXBean(description = "Connection health for one TAK server")
public class TakServerJMX {

  private final TakSocketConnection connection;
  private final ObjectInstance mbean;

  TakServerJMX(TakSocketConnection connection, String role) {
    this.connection = connection;
    // ':' is not allowed in an unquoted ObjectName value, so host and port are joined with '_'.
    this.mbean = JMXManager.getInstance().register(this, List.of(
        "type=Integration",
        "name=TakServer",
        "server=" + connection.getHost() + "_" + connection.getPort(),
        "role=" + role));
  }

  void close() {
    JMXManager.getInstance().unregister(mbean);
  }

  @JMXBeanAttribute(name = "Connected Count", description = "1 while the connection to this TAK server is open, otherwise 0")
  public long getConnectedCount() {
    return connection.isConnected() ? 1L : 0L;
  }

  @JMXBeanAttribute(name = "Connect Count", description = "Successful connections to this TAK server")
  public long getConnectCount() {
    return connection.getServerStats().getConnectCount();
  }

  @JMXBeanAttribute(name = "Connect Failure Count", description = "Failed connection attempts to this TAK server")
  public long getConnectFailureCount() {
    return connection.getServerStats().getConnectFailureCount();
  }

  @JMXBeanAttribute(name = "Disconnect Count", description = "Connections to this TAK server that were closed")
  public long getDisconnectCount() {
    return connection.getServerStats().getDisconnectCount();
  }

  @JMXBeanAttribute(name = "Dropped Count", description = "Events dropped because this server's send queue was full")
  public long getDroppedCount() {
    return connection.getServerStats().getDroppedCount();
  }

  @JMXBeanAttribute(name = "Write Count", description = "Events written to this TAK server")
  public long getWriteCount() {
    return connection.getServerStats().getWriteCount();
  }

  @JMXBeanAttribute(name = "Last Write Age Millis", description = "Milliseconds since the last write to this TAK server, -1 if none yet")
  public long getLastWriteAgeMillis() {
    return connection.getServerStats().getLastWriteAgeMillis();
  }

  @JMXBeanAttribute(name = "Queue Size", description = "Events waiting to be written to this TAK server")
  public long getQueueSize() {
    return connection.getQueue().size();
  }
}
