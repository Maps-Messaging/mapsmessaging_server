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

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.api.transformers.InterServerTransformation;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.analytics.StatisticsConfigDTO;
import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;
import io.mapsmessaging.utilities.filtering.NamespaceFilters;

import java.io.IOException;
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
      String local = property.getLocalNamespace();
      String remote = property.getRemoteNamespace();
      String selector = property.getSelector();
      boolean schema = property.isIncludeSchema();
      NamespaceFilters filters = property.getNamespaceFilters();
      QualityOfService qos = property.getQualityOfService();
      InterServerTransformation interServerTransformation = null;
      Map<String, Object> obj = property.getTransformer();
      if (obj != null && !obj.isEmpty()) {
        interServerTransformation = TransformerManager.getInstance().get(new ConfigurationProperties(obj));
      }

      try {
        if (direction.equalsIgnoreCase("pull")) {
          subscribeRemote(remote, local, qos, selector, interServerTransformation, schema, property.getStatistics());
        } else if (direction.equalsIgnoreCase("push")) {
          if (remote.endsWith("#")) {
            remote = remote.substring(0, remote.length() - 1);
          }
          subscribeLocal(local, remote, selector, qos, interServerTransformation, schema, filters, property.getStatistics());
        }
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED, direction, local, remote);
      } catch (IOException ioException) {
        success = false;
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_FAILED, direction, local, remote, ioException);
      }
    }
    return success;
  }

  private void subscribeLocal(String local, String remote, String selector, QualityOfService qos, InterServerTransformation interServerTransformation, boolean includeSchema, NamespaceFilters filters, StatisticsConfigDTO statistics) throws IOException {
    endPointConnection.getProtocol().subscribeLocal(local, remote, qos, selector, interServerTransformation, filters, statistics);
    if (includeSchema) {
      endPointConnection.getProtocol().subscribeLocal(constructSchema(local), constructSchema(remote), qos, selector, interServerTransformation, filters, null);
    }
  }

  private void subscribeRemote(String remote, String local, QualityOfService qos, String selector, InterServerTransformation interServerTransformation, boolean includeSchema, StatisticsConfigDTO statistics) throws IOException {
    ParserExecutor parser = null;
    if (selector != null && !selector.isEmpty()) {
      try {
        parser = SelectorParser.compile(selector);
      } catch (Throwable e) {
        throw new IOException("Unable to parse selector", e);
      }
    }
    endPointConnection.getProtocol().subscribeRemote(remote, local,  qos, parser, interServerTransformation, statistics);
    if (includeSchema) {
      endPointConnection.getProtocol().subscribeRemote(constructSchema(remote), constructSchema(local), qos, null, interServerTransformation, null);
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
}