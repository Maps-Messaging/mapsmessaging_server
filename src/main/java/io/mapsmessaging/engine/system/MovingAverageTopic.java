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
import io.mapsmessaging.utilities.stats.MovingAverage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MovingAverageTopic extends SystemTopic {

  private final MovingAverage movingAverage;
  private final DataSource dataSource;
  private final boolean diff;
  private long previous;

  public MovingAverageTopic(String topicName, int time, TimeUnit timeUnit, DataSource source, boolean diff) throws IOException {
    super(topicName);
    this.diff = diff;
    dataSource = source;
    movingAverage = new MovingAverage(time, timeUnit);
    previous = 0;
  }

  @Override
  public String getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }

  @Override
  protected Message generateMessage() {
    long value = dataSource.getData();
    if (diff) {
      value = value - previous;
      previous = value;
    }
    movingAverage.add(value);
    return getMessage(("" + movingAverage.getAverage()).getBytes());
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