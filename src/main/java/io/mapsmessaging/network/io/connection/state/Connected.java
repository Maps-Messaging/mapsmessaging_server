/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.api.transformers.Transformer;
import io.mapsmessaging.engine.transformers.TransformerManager;
import io.mapsmessaging.logging.ServerLogMessages;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.utilities.configuration.ConfigurationProperties;

import java.io.IOException;
import java.util.List;

public class Connected extends State {

  public Connected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    boolean failed = false;
    List<ConfigurationProperties> properties = endPointConnection.getDestinationMappings();
    for (ConfigurationProperties property : properties) {
      String direction = property.getProperty("direction");
      String local = property.getProperty("local_namespace");
      String remote = property.getProperty("remote_namespace");
      String selector = property.getProperty("selector");

      Transformer transformer = null;
      Object obj = property.get("transformer");
      if(obj instanceof ConfigurationProperties) {
        transformer = TransformerManager.getInstance().get((ConfigurationProperties)obj);
      }

      try {
        if (direction.equalsIgnoreCase("pull")) {
          endPointConnection.getConnection().subscribeRemote(remote, local, transformer);
        } else if (direction.equalsIgnoreCase("push")) {
          if(remote.endsWith("#")){
            remote = remote.substring(0, remote.length()-1);
          }
          endPointConnection.getConnection().subscribeLocal(local, remote, selector, transformer);
        }
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_ESTABLISHED, direction, local, remote);
      } catch (IOException ioException) {
        failed = true;
        endPointConnection.getLogger().log(ServerLogMessages.END_POINT_CONNECTION_SUBSCRIPTION_FAILED, direction, local, remote, ioException);
      }
    }
    if (!failed) {
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

  @Override
  public String getName() {
    return "Connected";
  }

}
