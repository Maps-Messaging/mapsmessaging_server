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
import io.mapsmessaging.dto.helpers.StatusMessageHelper;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;

import java.io.IOException;
import java.util.UUID;

public class ServerDetailsTopic extends SystemTopic {

  public ServerDetailsTopic() throws IOException {
    super("$SYS/server/details");
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
    ServerInfoDTO statusMessage = StatusMessageHelper.fromMessageDaemon(MessageDaemon.getInstance());
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


}
