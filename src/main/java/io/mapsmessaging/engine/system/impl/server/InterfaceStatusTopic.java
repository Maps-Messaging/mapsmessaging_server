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

package io.mapsmessaging.engine.system.impl.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.dto.helpers.InterfaceStatusHelper;
import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;
import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InterfaceStatusTopic extends SystemTopic {

  public InterfaceStatusTopic() throws IOException {
    super("$SYS/server/interface/status");
  }

  @Override
  public boolean isAdvanced() {
    return true;
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_JSON_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    InterfaceStatusMessage statusMessage = new InterfaceStatusMessage(MessageDaemon.getInstance());
    ObjectMapper mapper = new ObjectMapper();
    try {
      String jsonString = mapper.writeValueAsString(statusMessage);
      return getMessage(jsonString.getBytes());
    } catch (Exception e) {
      return getMessage(e.getMessage().getBytes());
    }
  }

  @Override
  public boolean hasUpdates() {
    return true;
  }

  @Override
  public String[] aliases() {
    return new String[]{};
  }

  @Data
  public static class InterfaceStatusMessage{
    private final List<InterfaceStatusDTO> interfaceStatusList;

    public InterfaceStatusMessage(){
      interfaceStatusList = new ArrayList<>();
    }

    public InterfaceStatusMessage(MessageDaemon messageDaemon) {
      interfaceStatusList = new ArrayList<>();
      for (EndPointManager endPointManager : messageDaemon.getSubSystemManager().getNetworkManager().getAll()) {
        interfaceStatusList.add(InterfaceStatusHelper.fromServer(endPointManager.getEndPointServer()));
      }
      for(EndPointConnection endPointConnection:messageDaemon.getSubSystemManager().getNetworkConnectionManager().getEndPointConnectionList()){
        interfaceStatusList.add(InterfaceStatusHelper.fromConnection(endPointConnection));
      }
    }
  }
}
