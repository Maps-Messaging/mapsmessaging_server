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

package io.mapsmessaging.engine.system;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.engine.schema.SchemaManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class SystemTopicWithAverage extends SystemTopic implements DataSource {

  private final List<SystemTopic> movingAverageTopics;
  private long lastUpdate;

  protected SystemTopicWithAverage(String name, boolean diff) throws IOException {
    super(name);
    updateSchema(SchemaManager.getInstance().getSchema(SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA), null);
    movingAverageTopics = new ArrayList<>();
    if(SystemTopicManager.isEnableStatistics()) {
      movingAverageTopics.add(new MovingAverageTopic(name + "/1", 1, TimeUnit.MINUTES, this, diff));
      movingAverageTopics.add(new MovingAverageTopic(name + "/5", 5, TimeUnit.MINUTES, this, diff));
      movingAverageTopics.add(new MovingAverageTopic(name + "/10", 10, TimeUnit.MINUTES, this, diff));
      movingAverageTopics.add(new MovingAverageTopic(name + "/15", 15, TimeUnit.MINUTES, this, diff));
    }
    lastUpdate = 0;
  }

  @Override
  public List<SystemTopic> getChildren() {
    return movingAverageTopics;
  }

  @Override
  public boolean hasUpdates() {
    long next = getData();
    if (next != lastUpdate) {
      lastUpdate = next;
      return true;
    }
    return false;
  }

  @Override
  public UUID getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    return getMessage(("" + lastUpdate).getBytes());
  }
}
