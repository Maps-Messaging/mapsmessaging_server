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
import io.mapsmessaging.dto.helpers.DestinationStatusHelper;
import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DestinationStatusTopic extends SystemTopic {

  public DestinationStatusTopic() throws IOException {
    super("$SYS/server/destination/status");
  }

  @Override
  public boolean isAdvanced() {
    return true;
  }

  @Override
  public String getSchemaUUID() {
    return SchemaManager.DEFAULT_JSON_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    DestinationStatusMessage statusMessage = new DestinationStatusMessage(MessageDaemon.getInstance().getDestinationManager().getAll());
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
  @NoArgsConstructor
  public static final class DestinationStatusMessage{
    private List<DestinationDTO> destinationStatusList;

    public DestinationStatusMessage(List<String> destinations){
      destinationStatusList = new ArrayList<>();
      for(String destinationName:destinations){
        try {
          DestinationImpl destination = MessageDaemon.getInstance().getDestinationManager().find(destinationName).get();
          if (destination != null && !destination.getFullyQualifiedNamespace().startsWith("$SYS")) {
            destinationStatusList.add(DestinationStatusHelper.createDestination(destination));
          }
        } catch(InterruptedException | ExecutionException e){
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }

      }
    }

  }

}
