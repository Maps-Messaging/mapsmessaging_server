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

import static org.maps.network.io.connection.Constants.DELAYED_TIME;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.ThreadContext;
import org.maps.logging.LogMessages;
import org.maps.network.EndPointURL;
import org.maps.network.io.EndPoint;
import org.maps.network.io.EndPointConnectedCallback;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.network.io.impl.SelectorLoadManager;
import org.maps.network.protocol.ProtocolFactory;
import org.maps.network.protocol.ProtocolImpl;
import org.maps.network.protocol.ProtocolImplFactory;

public class Disconnected extends State implements EndPointConnectedCallback {

  private EndPoint activeEndPoint;

  public Disconnected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void connected(EndPoint endpoint) {
    String protocol = endPointConnection.getProperties().getProperty("protocol");
    String url = endPointConnection.getUrl().toString();
    try {
      ThreadContext.put("endpoint", url);
      ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
      ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
      ProtocolImpl protocolImpl =  protocolImplFactory.connect(endpoint);
      endPointConnection.setConnection(protocolImpl);
      endPointConnection.scheduleState(new Connecting(endPointConnection));
    } catch (IOException ioException) {
      endPointConnection.getLogger().log(LogMessages.END_POINT_CONNECTION_PROTOCOL_FAILED, url, protocol, ioException);
      endPointConnection.scheduleState(new Delayed(endPointConnection), DELAYED_TIME);
    }
  }

  @Override
  public void execute() {
    EndPointURL url = endPointConnection.getUrl();
    try {
      SelectorLoadManager selectorLoadManager = endPointConnection.getSelectorLoadManager();
      List<String> jmxPath = endPointConnection.getJMXPath();
      activeEndPoint = endPointConnection.getEndPointConnectionFactory().connect(url, selectorLoadManager, this, endPointConnection, jmxPath);
    } catch (Throwable ioException) {
      endPointConnection.getLogger().log(LogMessages.END_POINT_CONNECTION_FAILED, url, ioException);
      endPointConnection.scheduleState(new Delayed(endPointConnection), DELAYED_TIME);
    }
  }

  @Override
  public void cancel() {
    if(activeEndPoint != null){
      try {
        activeEndPoint.close();
      } catch (IOException ioException) {
        // we are closing it, not too fussed about an exception here
      }
    }
  }

  @Override
  public String getName() {
    return "Disconnected";
  }

}
