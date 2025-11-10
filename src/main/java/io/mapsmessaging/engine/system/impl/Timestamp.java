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

package io.mapsmessaging.engine.system.impl;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopic;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

public class Timestamp extends SystemTopic {

  public Timestamp() throws IOException {
    super("$SYS/broker/time");
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_STRING_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    return getMessage(LocalDateTime.now().toString().getBytes());
  }

  @Override
  public boolean hasUpdates() {
    return true;
  }

  @Override
  public String[] aliases() {
    return new String[]{"$SYS/broker/timestamp"};
  }
}
