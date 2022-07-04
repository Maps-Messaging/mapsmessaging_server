/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.system.impl.messages.publish;

import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopicWithAverage;
import java.io.IOException;
import java.util.UUID;

public class Dropped extends SystemTopicWithAverage {

  public Dropped() throws IOException {
    super("$SYS/broker/publish/messages/dropped", true);
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }

  @Override
  public long getData() {
    return DestinationStats.getTotalNoInterestMessages();
  }

  @Override
  public String[] aliases() {
    return new String[]{
        "$SYS/broker/messages/publish/dropped",
        "$SYS/messages/publish/dropped",
        "$SYS/load/publish/dropped"
    };
  }
}
