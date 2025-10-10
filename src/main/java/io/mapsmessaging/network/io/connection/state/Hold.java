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

import io.mapsmessaging.dto.rest.config.protocol.LinkConfigDTO;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;

import java.io.IOException;
import java.util.List;

public class Hold extends State {

  public Hold(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    processLinkRequests(endPointConnection.getProperties().getLinkConfigs());
    endPointConnection.scheduleState(new Holding(endPointConnection));
  }

  @Override
  public String getName() {
    return "Hold";
  }

  @Override
  public LinkState getLinkState() {
    return LinkState.CONNECTED;
  }


  private boolean processLinkRequests(List<LinkConfigDTO> linkConfigs) {
    boolean success = true;
    for (LinkConfigDTO property : linkConfigs) {
      String direction = property.getDirection();
      String local = property.getLocalNamespace();
      String remote = property.getRemoteNamespace();
      boolean schema = property.isIncludeSchema();
      try {
        if (direction.equalsIgnoreCase("pull")) {
          unsubscribeRemote(remote, schema);
        } else if (direction.equalsIgnoreCase("push")) {
          if (remote.endsWith("#")) {
            remote = remote.substring(0, remote.length() - 1);
          }
          unsubscribeLocal(local, schema);
        }
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED, direction, local, remote);
      } catch (IOException ioException) {
        success = false;
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_FAILED, direction, local, remote, ioException);
      }
    }
    return success;
  }

  private void unsubscribeLocal(String local, boolean includeSchema) throws IOException {
    endPointConnection.getConnection().unsubscribeLocal(local);
    if (includeSchema) {
      endPointConnection.getConnection().unsubscribeLocal(constructSchema(local));
    }
  }

  private void unsubscribeRemote(String remote, boolean includeSchema) throws IOException {
    endPointConnection.getConnection().unsubscribeRemote(remote);
    if (includeSchema) {
      endPointConnection.getConnection().unsubscribeRemote(constructSchema(remote));
    }
  }

}