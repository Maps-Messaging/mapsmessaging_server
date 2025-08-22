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

import io.mapsmessaging.api.features.DestinationMode;
import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Connected extends State {

  public Connected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    if (processLinkRequests(endPointConnection.getProperties().getLinkConfigs())) {
      endPointConnection.scheduleState(new Established(endPointConnection));
    } else {
      try {
        endPointConnection.getConnection().close();
      } catch (IOException ioException) {
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, ioException);
      }
      endPointConnection.scheduleState(new Disconnected(endPointConnection));
    }
  }

  private boolean processLinkRequests(List<LinkConfigDTO> linkConfigs){
    boolean success = true;
    for (LinkConfigDTO property : linkConfigs) {
      String direction = property.getDirection();
      String local = property.getLocalNamespace();
      String remote = property.getRemoteNamespace();
      String selector = property.getSelector();
      boolean schema = property.isIncludeSchema();
      NamespaceFilters filters = property.getNamespaceFilters();

      Transformer transformer = null;
      Map<String, Object> obj = property.getTransformer();
      if(obj != null && !obj.isEmpty()) {
        transformer = TransformerManager.getInstance().get(new ConfigurationProperties(obj));
      }

      try {
        if (direction.equalsIgnoreCase("pull")) {
          subscribeRemote(remote, local, selector, transformer, schema);
        } else if (direction.equalsIgnoreCase("push")) {
          if(remote.endsWith("#")){
            remote = remote.substring(0, remote.length()-1);
          }
          subscribeLocal(local, remote, selector, transformer, schema, filters);
        }
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED, direction, local, remote);
      } catch (IOException ioException) {
        success = false;
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_FAILED, direction, local, remote, ioException);
      }
    }
    return success;
  }

  private void subscribeLocal(String local, String remote, String selector, Transformer transformer, boolean includeSchema, NamespaceFilters filters) throws IOException {
    endPointConnection.getConnection().subscribeLocal(local, remote, selector, transformer, filters);
    if(includeSchema){
      endPointConnection.getConnection().subscribeLocal(constructSchema(local), constructSchema(remote), selector, transformer,filters );
    }
  }

  private void subscribeRemote(String remote, String local, String selector, Transformer transformer, boolean includeSchema) throws IOException {
    ParserExecutor parser = null;
    if(selector != null && !selector.isEmpty()){
      try {
        parser = SelectorParser.compile(selector);
      } catch (Throwable e) {
        throw new IOException("Unable to parse selector", e);
      }
    }
    endPointConnection.getConnection().subscribeRemote(remote, local, parser, transformer);
    if(includeSchema){
      endPointConnection.getConnection().subscribeRemote(constructSchema(remote), constructSchema(local), null, transformer);
    }
  }

  @Override
  public String getName() {
    return "Connected";
  }

  private String constructSchema(String namespace){
    if(!namespace.startsWith("/")){
      namespace = "/"+namespace;
    }
    return DestinationMode.SCHEMA.getNamespace() + namespace;
  }

}
