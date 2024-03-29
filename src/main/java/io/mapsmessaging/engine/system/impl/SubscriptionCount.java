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

package io.mapsmessaging.engine.system.impl;

import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.engine.system.SystemTopicWithAverage;

import java.io.IOException;

public class SubscriptionCount extends SystemTopicWithAverage {

  public SubscriptionCount() throws IOException {
    super("$SYS/broker/subscriptions/count", false);
  }

  @Override
  public String getSchemaUUID() {
    return SchemaManager.DEFAULT_NUMERIC_STRING_SCHEMA;
  }

  @Override
  public long getData() {
    return DestinationStats.getTotalCurrentSubscriptions();
  }

  @Override
  public String[] aliases() {
    return new String[]{"$SYS/subscriptions/count"};
  }
}
