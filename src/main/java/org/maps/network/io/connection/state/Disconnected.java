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

package org.maps.network.io.connection.state;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ThreadContext;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.network.protocol.ProtocolFactory;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.ProtocolImplFactory;
import org.maps.utilities.threads.SimpleTaskScheduler;

public class Disconnected extends State implements EndPointConnectedCallback {

  public Disconnected(EndPointConnection connection) {
    super(connection);
  }

  public void establishConnection() throws IOException {
    endPointConnection.getEndPointConnectionFactory().connect(endPointConnection.getUrl(), endPointConnection.getSelectorLoadManager(), this, endPointConnection, endPointConnection.getJMXPath());
  }

  public ProtocolImpl accept(EndPoint endpoint) throws IOException {
    ThreadContext.put("endpoint", endPointConnection.getUrl().toString());
    String protocol = endPointConnection.getProperties().getProperty("protocol");
    ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
    ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
    return protocolImplFactory.connect(endpoint);
  }

  @Override
  public void execute() {
    try {
      establishConnection();
    } catch (Throwable ioException) {
      setState(new Delayed(endPointConnection));
      SimpleTaskScheduler.getInstance().schedule(endPointConnection.getState(), 10, TimeUnit.SECONDS);
    }
  }

  @Override
  public void connected(EndPoint endpoint) {
    try {
      ProtocolImpl protocol = accept(endpoint);
      endPointConnection.setConnection(protocol);
      setState(new Connecting(endPointConnection));
    } catch (IOException ioException) {
      setState(new Delayed(endPointConnection));
      SimpleTaskScheduler.getInstance().schedule(endPointConnection.getState(), 10, TimeUnit.SECONDS);
    }
  }
}
