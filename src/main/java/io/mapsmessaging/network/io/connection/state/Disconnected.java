/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.config.network.EndPointConnectionServerConfig;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.logging.ThreadContext;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.auth.TokenGenerator;
import io.mapsmessaging.network.auth.TokenGeneratorManager;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointConnectedCallback;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.io.impl.SelectorLoadManager;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.ProtocolFactory;
import io.mapsmessaging.network.protocol.ProtocolImplFactory;
import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;
import io.mapsmessaging.network.protocol.transformation.TransformationManager;

import java.io.IOException;
import java.util.List;

import static io.mapsmessaging.network.io.connection.Constants.DELAYED_TIME;

public class Disconnected extends State implements EndPointConnectedCallback {

  private EndPoint activeEndPoint;

  public Disconnected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void connected(EndPoint endpoint) {
    String protocol = endPointConnection.getProperties().getProtocols();
    String url = endPointConnection.getUrl().toString();
    try {
      ThreadContext.put("endpoint", url);
      ProtocolFactory protocolFactory = new ProtocolFactory(protocol);
      ProtocolImplFactory protocolImplFactory = protocolFactory.getBoundedProtocol();
      EndPointConnectionServerConfig properties = (EndPointConnectionServerConfig) endpoint.getConfig();

      String sessionId = properties.getAuthConfig().getSessionId();
      String username = properties.getAuthConfig().getUsername();
      String password = properties.getAuthConfig().getPassword();
      String tokenGeneratorName = properties.getAuthConfig().getTokenGenerator();
      String transformationName = properties.getLinkTransformation();
      ProtocolMessageTransformation transformation = TransformationManager.getInstance().getTransformation(transformationName);
      if(transformation == null) {
        transformation = TransformationManager.getInstance().getTransformation(endpoint.getProtocol(), endPointConnection.getUrl().getHost(), protocolImplFactory.getName(), username );
      }

      if(transformation != null) {
        sessionId = sessionId+"?Transformation="+transformation.getName();
      }
      if (tokenGeneratorName != null && !tokenGeneratorName.isEmpty()) {
        TokenGenerator tokenGenerator = TokenGeneratorManager.getInstance().get(tokenGeneratorName).getInstance(properties.getAuthConfig().getTokenConfig());
        password = tokenGenerator.generate();
      }
      Protocol protocolImpl = protocolImplFactory.connect(endpoint, sessionId, username, password);
      protocolImpl.setTransformation(transformation);
      endPointConnection.setConnection(protocolImpl);
      endPointConnection.scheduleState(new Connecting(endPointConnection));
    } catch (IOException ioException) {
      endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_PROTOCOL_FAILED, url, protocol, ioException);
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
    } catch (Exception ioException) {
      endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_FAILED, url, ioException);
      endPointConnection.scheduleState(new Delayed(endPointConnection), DELAYED_TIME);
    }
  }

  @Override
  public void cancel() {
    if (activeEndPoint != null) {
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
