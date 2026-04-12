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

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.InterServerPipelineTransformation;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.NamespaceFilterDTO;
import io.mapsmessaging.dto.rest.config.transformer.TransformationConfigDTO;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Establishing extends State {

  public Establishing(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    if (processLinkRequests(endPointConnection.getProperties().getLinkConfigs())) {
      endPointConnection.scheduleState(new Established(endPointConnection));
    } else {
      try {
        endPointConnection.getProtocol().close();
      } catch (IOException ioException) {
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_CLOSE_EXCEPTION, ioException);
      }
      endPointConnection.scheduleState(new Disconnected(endPointConnection));
    }
  }

  private boolean processLinkRequests(List<LinkConfigDTO> linkConfigs) {
    boolean success = true;
    for (LinkConfigDTO property : linkConfigs) {
      String direction = property.getDirection();
      String remote = property.getRemoteNamespace();
      try {
        if (direction.equalsIgnoreCase("pull")) {
          subscribeRemote(property);
        } else if (direction.equalsIgnoreCase("push")) {
          if (remote.endsWith("#")) {
            remote = remote.substring(0, remote.length() - 1);
          }
          subscribeLocal( property, remote);
        }
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED, direction, property.getLocalNamespace(), remote);
      } catch (IOException ioException) {
        success = false;
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_FAILED, direction, property.getLocalNamespace(), remote, ioException);
      }
    }
    return success;
  }

  private void subscribeLocal( LinkConfigDTO property, String remote) throws IOException {
    InterServerTransformation interServerTransformation = buildPipeLine(property);
    String local = property.getLocalNamespace();
    String selector = property.getSelector();
    boolean schema = property.isIncludeSchema();
    List<NamespaceFilterDTO> filters = property.getNamespaceFilters();
    NamespaceFilters namespaceFilters = null;
    if(filters !=null && !filters.isEmpty()) {
      namespaceFilters  = new NamespaceFilters(filters);
    }
    QualityOfService qos = property.getQualityOfService();

    Map<String, Object> linkProperties = property.getLinkProperties();
    endPointConnection.getProtocol().subscribeLocal(local, remote, qos, selector, interServerTransformation, namespaceFilters, property.getStatistics(), linkProperties);
    if (schema) {
      endPointConnection.getProtocol().subscribeLocal(constructSchema(local), constructSchema(remote), qos, selector, interServerTransformation, namespaceFilters, null, linkProperties);
    }
  }

  private void subscribeRemote(LinkConfigDTO property) throws IOException {
    String remote = property.getRemoteNamespace();
    String local = property.getLocalNamespace();
    String selector = property.getSelector();
    boolean schema = property.isIncludeSchema();

    InterServerTransformation interServerTransformation = buildPipeLine(property);
    QualityOfService qos = property.getQualityOfService();

    ParserExecutor parser = null;
    if (selector != null && !selector.isEmpty()) {
      try {
        parser = SelectorParser.compile(selector);
      } catch (Throwable e) {
        throw new IOException("Unable to parse selector", e);
      }
    }
    Map<String, Object> linkProperties = property.getLinkProperties();
    endPointConnection.getProtocol().subscribeRemote(remote, local,  qos, parser, interServerTransformation, property.getStatistics(), linkProperties);
    if (schema) {
      endPointConnection.getProtocol().subscribeRemote(constructSchema(remote), constructSchema(local), qos, null, interServerTransformation, null, linkProperties);
    }
  }

  @Override
  public String getName() {
    return "Establishing";
  }


  @Override
  public LinkState getLinkState() {
    return LinkState.CONNECTED;
  }

  private InterServerPipelineTransformation buildPipeLine(LinkConfigDTO property){
    List<InterServerTransformation> interServerTransformation = new ArrayList<>();
    if(property.getTransformer() != null && !property.getTransformer().isEmpty()){
      for(TransformationConfigDTO dto : property.getTransformer()){
        InterServerTransformation t = TransformerManager.getInstance().get(dto);
        if(t != null){
          interServerTransformation.add(t);
        }
      }
    }
    return new InterServerPipelineTransformation(interServerTransformation);
  }
}